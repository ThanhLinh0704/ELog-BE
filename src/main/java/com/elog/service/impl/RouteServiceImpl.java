package com.elog.service.impl;

import com.elog.dto.request.*;
import com.elog.dto.response.*;
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
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final StoreRepository storeRepository;
    private final RouteMapper routeMapper;

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
        List<RouteResponse> content = page.getContent().stream()
                .map(r -> routeMapper.toResponse(r, routeStopRepository.countByRouteId(r.getId())))
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

        routeStopRepository.delete(stop);

        // Renumber remaining stops
        List<RouteStop> remaining = routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(routeId);
        int seq = 1;
        for (RouteStop rs : remaining) {
            rs.setSequenceOrder(seq++);
            routeStopRepository.save(rs);
        }
    }

    // ── helpers ──────────────────────────────────────────────

    private Route findRouteOrThrow(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ROUTE_NOT_FOUND, "Route not found: " + id, HttpStatus.NOT_FOUND));
    }
}
