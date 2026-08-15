package com.elog.service.impl;

import com.elog.dto.request.route.RouteCreateRequest;
import com.elog.dto.request.route.RouteStatusUpdateRequest;
import com.elog.dto.request.route.RouteStopAddRequest;
import com.elog.dto.request.route.RouteStopReorderRequest;
import com.elog.dto.request.route.RouteUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.route.RouteDetailResponse;
import com.elog.dto.response.route.RouteDirectionsResponse;
import com.elog.dto.response.route.RouteResponse;
import com.elog.dto.response.route.RouteStopResponse;
import com.elog.entity.Route;
import com.elog.entity.RouteStop;
import com.elog.entity.Store;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.RouteMapper;
import com.elog.repository.RouteRepository;
import com.elog.repository.RouteStopRepository;
import com.elog.repository.StoreRepository;
import com.elog.repository.specification.RouteSpecification;
import com.elog.service.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final StoreRepository storeRepository;
    private final RouteMapper routeMapper;
    private final com.elog.repository.TripStopRepository tripStopRepository;
    private final com.elog.repository.TripDraftStopRepository tripDraftStopRepository;
    private final com.elog.repository.ManifestLineRepository manifestLineRepository;
    private final com.elog.repository.DeliveryOrderResultRepository deliveryOrderResultRepository;
    private final com.elog.service.GoongMapService goongMapService;

    // ── Route CRUD ──────────────────────────────────────────────

    @Override
    @Transactional
    public RouteResponse createRoute(RouteCreateRequest request) {
        String code = request.getCode().toUpperCase();
        if (routeRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.ROUTE_CODE_DUPLICATE,
                    "Route code already exists: " + code, HttpStatus.CONFLICT);
        }

        Route route = Route.builder()
                .code(code)
                .name(request.getName())
                .description(request.getDescription())
                .isActive(false)
                .build();
        Route saved = routeRepository.save(route);
        return routeMapper.toResponse(saved, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteDetailResponse getRouteById(Long id) {
        Route route = findRouteOrThrow(id);
        List<RouteStop> stops = routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(id);
        return routeMapper.toDetailResponse(route, stops);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<RouteResponse>> getAllRoutes(
            String keyword, Boolean isActive, Pageable pageable) {

        Specification<Route> spec = Specification
                .where(RouteSpecification.hasKeyword(keyword))
                .and(RouteSpecification.hasActiveStatus(isActive));

        Page<Route> page = routeRepository.findAll(spec, pageable);

        List<Long> routeIds = page.getContent().stream().map(Route::getId).toList();
        List<Object[]> rawCounts = routeIds.isEmpty()
                ? java.util.Collections.emptyList()
                : routeStopRepository.countStopsByRouteIdIn(routeIds);

        java.util.Map<Long, Integer> countMap = rawCounts.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        List<RouteResponse> content = page.getContent().stream()
                .map(r -> routeMapper.toResponse(r, countMap.getOrDefault(r.getId(), 0)))
                .toList();

        ApiResponse.PaginationInfo pagination = ApiResponse.PaginationInfo.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return ApiResponse.<List<RouteResponse>>builder()
                .success(true)
                .data(content)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional
    public RouteResponse updateRoute(Long id, RouteUpdateRequest request) {
        Route route = findRouteOrThrow(id);
        route.setName(request.getName());
        route.setDescription(request.getDescription());
        Route saved = routeRepository.save(route);
        return routeMapper.toResponse(saved, routeStopRepository.countByRouteId(id));
    }

    @Override
    @Transactional
    public RouteResponse updateRouteStatus(Long id, RouteStatusUpdateRequest request) {
        Route route = findRouteOrThrow(id);

        if (Boolean.TRUE.equals(request.getIsActive())) {
            int stopCount = routeStopRepository.countByRouteId(id);
            if (stopCount < 2) {
                throw new BusinessException(ErrorCode.ROUTE_INSUFFICIENT_STOPS,
                        "Route must have at least 2 stops to activate. Current: " + stopCount,
                        HttpStatus.BAD_REQUEST);
            }
        }

        route.setIsActive(request.getIsActive());
        Route saved = routeRepository.save(route);
        return routeMapper.toResponse(saved, routeStopRepository.countByRouteId(id));
    }

    // ── Stop Management ─────────────────────────────────────────

    @Override
    @Transactional
    public RouteStopResponse addStop(Long routeId, RouteStopAddRequest request) {
        Route route = findRouteOrThrow(routeId);

        // Validate store exists
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND,
                        "Store not found: " + request.getStoreId(), HttpStatus.NOT_FOUND));

        // BR-02: store must be active
        if (!Boolean.TRUE.equals(store.getIsActive())) {
            throw new BusinessException(ErrorCode.STORE_INACTIVE,
                    "Store is inactive: " + store.getCode(), HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Check duplicate store in route
        if (routeStopRepository.existsByRouteIdAndStoreId(routeId, request.getStoreId())) {
            throw new BusinessException(ErrorCode.ROUTE_STOP_DUPLICATE,
                    "Store " + store.getCode() + " already exists in this route",
                    HttpStatus.CONFLICT);
        }

        // Auto-append to end
        int nextSeq = routeStopRepository.findMaxSequenceOrderByRouteId(routeId) + 1;

        RouteStop routeStop = RouteStop.builder()
                .route(route)
                .store(store)
                .sequenceOrder(nextSeq)
                .build();
        RouteStop saved = routeStopRepository.save(routeStop);

        route.setRoutePolyline(null);
        routeRepository.save(route);

        boolean hasCoords = store.getLatitude() != null && store.getLongitude() != null;
        return routeMapper.toStopResponse(saved, !hasCoords);
    }

    @Override
    @Transactional
    public List<RouteStopResponse> reorderStops(Long routeId, RouteStopReorderRequest request) {
        findRouteOrThrow(routeId);

        List<RouteStop> existingStops = routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(routeId);
        Set<Long> existingIds = existingStops.stream()
                .map(RouteStop::getId)
                .collect(Collectors.toSet());
        Set<Long> requestedIds = new LinkedHashSet<>(request.getOrderedStopIds());

        // Validate: must contain exactly the same stop IDs
        if (!existingIds.equals(requestedIds)) {
            throw new BusinessException(ErrorCode.ROUTE_STOP_REORDER_INVALID,
                    "orderedStopIds must contain exactly all stop IDs of this route",
                    HttpStatus.BAD_REQUEST);
        }

        // Build lookup map
        Map<Long, RouteStop> stopMap = existingStops.stream()
                .collect(Collectors.toMap(RouteStop::getId, s -> s));

        // Step 1: Move existing stops to a temporary sequence range to avoid unique constraint violations
        for (RouteStop rs : existingStops) {
            rs.setSequenceOrder(rs.getSequenceOrder() + 10000);
            routeStopRepository.save(rs);
        }
        routeStopRepository.flush(); // Force update in DB before reassigning

        // Step 2: Reassign sequence numbers in the requested order
        int seq = 1;
        List<RouteStopResponse> result = new ArrayList<>();
        for (Long stopId : request.getOrderedStopIds()) {
            RouteStop rs = stopMap.get(stopId);
            rs.setSequenceOrder(seq++);
            routeStopRepository.save(rs);

            boolean hasCoords = rs.getStore().getLatitude() != null
                    && rs.getStore().getLongitude() != null;
            result.add(routeMapper.toStopResponse(rs, !hasCoords));
        }

        Route route = findRouteOrThrow(routeId);
        route.setRoutePolyline(null);
        routeRepository.save(route);

        return result;
    }

    @Override
    @Transactional
    public void removeStop(Long routeId, Long stopId) {
        findRouteOrThrow(routeId);

        RouteStop stop = routeStopRepository.findById(stopId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_STOP_NOT_FOUND,
                        "Route stop not found: " + stopId, HttpStatus.NOT_FOUND));

        if (!stop.getRoute().getId().equals(routeId)) {
            throw new BusinessException(ErrorCode.ROUTE_STOP_NOT_FOUND,
                    "Stop " + stopId + " does not belong to route " + routeId,
                    HttpStatus.NOT_FOUND);
        }

        // 1. Clean up references prior to deleting RouteStop so admin can remove any store
        tripStopRepository.nullifyRouteStopId(stopId);
        tripStopRepository.nullifyTripDraftStopIdByRouteStopId(stopId);
        deliveryOrderResultRepository.deleteByRouteStopId(stopId);
        manifestLineRepository.deleteByRouteStopId(stopId);
        tripDraftStopRepository.deleteByRouteStopId(stopId);

        // 2. Delete the route stop
        routeStopRepository.delete(stop);

        // Renumber remaining stops
        List<RouteStop> remaining = routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(routeId);
        int seq = 1;
        for (RouteStop rs : remaining) {
            rs.setSequenceOrder(seq++);
            routeStopRepository.save(rs);
        }

        Route route = findRouteOrThrow(routeId);
        route.setRoutePolyline(null);
        routeRepository.save(route);
    }

    @Override
    @Transactional
    public RouteDirectionsResponse getRouteDirections(Long routeId) {
        return getRouteDirections(routeId, false);
    }

    @Override
    @Transactional
    public RouteDirectionsResponse getRouteDirections(Long routeId, boolean forceRefresh) {
        Route route = findRouteOrThrow(routeId);
        List<RouteStop> stops = routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(routeId);

        double warehouseLat = 21.032612;
        double warehouseLng = 105.868367;

        if (forceRefresh) {
            route.setRoutePolyline(null);
            route.setTotalDistanceKm(null);
            route.setTotalDurationMin(null);
            routeRepository.save(route);
        } else if (route.getRoutePolyline() != null && !route.getRoutePolyline().trim().isEmpty()) {
            return RouteDirectionsResponse.builder()
                    .routeId(route.getId())
                    .routeCode(route.getCode())
                    .routeName(route.getName())
                    .routePolyline(route.getRoutePolyline())
                    .totalDistanceKm(route.getTotalDistanceKm())
                    .totalDurationMin(route.getTotalDurationMin())
                    .warehouseLat(warehouseLat)
                    .warehouseLng(warehouseLng)
                    .build();
        }

        RouteDirectionsResponse.RouteDirectionsResponseBuilder builder = RouteDirectionsResponse.builder()
                .routeId(route.getId())
                .routeCode(route.getCode())
                .routeName(route.getName())
                .warehouseLat(warehouseLat)
                .warehouseLng(warehouseLng);

        List<RouteStop> validStops = stops.stream()
                .filter(s -> s.getStore() != null && s.getStore().getLatitude() != null && s.getStore().getLongitude() != null)
                .collect(Collectors.toList());

        if (validStops.isEmpty()) {
            return builder.totalDistanceKm(0.0).totalDurationMin(0).build();
        }

        List<double[]> coords = new ArrayList<>();
        coords.add(new double[]{warehouseLat, warehouseLng});
        for (RouteStop s : validStops) {
            coords.add(new double[]{s.getStore().getLatitude(), s.getStore().getLongitude()});
        }

        if (goongMapService != null && goongMapService.isConfigured()) {
            try {
                String origin = coords.get(0)[0] + "," + coords.get(0)[1];
                String destination = coords.get(coords.size() - 1)[0] + "," + coords.get(coords.size() - 1)[1];

                String waypoints = null;
                if (coords.size() > 2) {
                    waypoints = coords.subList(1, coords.size() - 1).stream()
                            .map(pt -> pt[0] + "," + pt[1])
                            .collect(Collectors.joining("|"));
                }

                var goongRes = goongMapService.getDirections(origin, destination, waypoints);
                if (goongRes != null && goongRes.getRoutes() != null && !goongRes.getRoutes().isEmpty()) {
                    var r = goongRes.getRoutes().get(0);
                    if (r.getOverviewPolyline() != null && r.getOverviewPolyline().getPoints() != null) {
                        String polyline = r.getOverviewPolyline().getPoints();
                        builder.routePolyline(polyline);
                        route.setRoutePolyline(polyline);
                    }
                    if (r.getLegs() != null) {
                        long totalMeters = r.getLegs().stream()
                                .mapToLong(leg -> leg.getDistance() != null && leg.getDistance().getValue() != null ? leg.getDistance().getValue() : 0)
                                .sum();
                        double distKm = totalMeters / 1000.0;
                        int durMin = (int) Math.round((distKm / 40.0) * 60.0);

                        builder.totalDistanceKm(distKm);
                        builder.totalDurationMin(durMin);

                        route.setTotalDistanceKm(distKm);
                        route.setTotalDurationMin(durMin);
                    }
                    routeRepository.save(route);
                } else {
                    log.warn("Goong directions API returned no routes or rate-limited for routeId={}", routeId);
                }
            } catch (Exception e) {
                log.error("Error building route directions: {}", e.getMessage(), e);
            }
        }

        return builder.build();
    }

    // ── helpers ──────────────────────────────────────────────

    private Route findRouteOrThrow(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ROUTE_NOT_FOUND, "Route not found: " + id, HttpStatus.NOT_FOUND));
    }
}
