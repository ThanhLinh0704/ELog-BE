package com.elog.service.impl;

import com.elog.dto.request.RecalculateEtaRequest;
import com.elog.dto.request.StopUpdateRequest;
import com.elog.dto.response.*;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.EtaCalculationService;
import com.elog.service.TripDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripDraftServiceImpl implements TripDraftService {

    private final OrderRepository orderRepository;
    private final RouteStopRepository routeStopRepository;
    private final TripDraftRepository tripDraftRepository;
    private final TripDraftStopRepository tripDraftStopRepository;
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final EtaCalculationService etaCalculationService;
    private final OrderItemRepository orderItemRepository;
    private final TripRepository tripRepository;
    private final ManifestRepository manifestRepository;
    private final com.elog.service.PlanningHistoryService planningHistoryService;

    @Override
    @Transactional
    public ConsolidateResponse consolidate(LocalDate deliveryDate) {
        // 1. Fetch all ACCEPTED orders for this date (active batch only)
        List<Order> acceptedOrders = orderRepository
                .findByDeliveryDateAndStatus(deliveryDate, "ACCEPTED");

        if (acceptedOrders.isEmpty()) {
            return ConsolidateResponse.builder()
                    .deliveryDate(deliveryDate)
                    .tripDraftsCreatedOrUpdated(0)
                    .tripDrafts(Collections.emptyList())
                    .skippedRoutes(Collections.emptyList())
                    .build();
        }

        // 2. Deduplicate orders (JOIN FETCH on items may produce duplicates)
        acceptedOrders = new ArrayList<>(
                acceptedOrders.stream()
                        .collect(Collectors.toMap(Order::getId, o -> o, (a, b) -> a,
                                LinkedHashMap::new))
                        .values());

        // 3. Build store → route mapping via route_stops table
        Set<Long> storeIds = acceptedOrders.stream()
                .map(o -> o.getStore().getId())
                .collect(Collectors.toSet());

        Map<Long, RouteStop> storeToRouteStop = new HashMap<>();
        for (Long storeId : storeIds) {
            routeStopRepository.findFirstByStoreId(storeId)
                    .ifPresent(rs -> storeToRouteStop.put(storeId, rs));
        }

        // 4. Group orders by route_id
        Map<Long, List<Order>> ordersByRoute = new LinkedHashMap<>();
        List<Order> unmappedOrders = new ArrayList<>();

        for (Order order : acceptedOrders) {
            RouteStop rs = storeToRouteStop.get(order.getStore().getId());
            if (rs != null) {
                ordersByRoute.computeIfAbsent(rs.getRoute().getId(), k -> new ArrayList<>())
                        .add(order);
            } else {
                unmappedOrders.add(order);
            }
        }

        if (!unmappedOrders.isEmpty()) {
            log.warn("US-10: {} orders have stores not mapped to any route (skipped)",
                    unmappedOrders.size());
        }

        // 5. Process each route
        List<TripDraftResponse> results = new ArrayList<>();
        List<ConsolidateResponse.SkippedRouteInfo> skippedRoutes = new ArrayList<>();

        for (Map.Entry<Long, List<Order>> entry : ordersByRoute.entrySet()) {
            Long routeId = entry.getKey();
            List<Order> routeOrders = entry.getValue();

            Route route = routeRepository.findById(routeId).orElse(null);
            if (route == null) {
                log.error("US-10: Route id={} not found in DB", routeId);
                continue;
            }

            // 5a. Guard: route must have RouteStops (NAC-10a)
            List<RouteStop> routeStops = routeStopRepository
                    .findByRouteIdOrderBySequenceOrderAsc(routeId);
            if (routeStops.isEmpty()) {
                log.warn("US-10 NAC-10a: Route {} has no RouteStops, skipping", route.getCode());
                skippedRoutes.add(ConsolidateResponse.SkippedRouteInfo.builder()
                        .routeId(routeId)
                        .routeCode(route.getCode())
                        .reason("Route reference data incomplete — no RouteStop defined")
                        .build());
                continue;
            }

            // 5b. Guard: if TripDraft exists and status != DRAFT → block (NAC-10b)
            TripDraft existing = tripDraftRepository
                    .findByRouteIdAndDeliveryDate(routeId, deliveryDate).orElse(null);
            if (existing != null && !"DRAFT".equals(existing.getStatus())) {
                throw new BusinessException(
                        ErrorCode.TRIP_DRAFT_LOCKED,
                        String.format("Trip Draft cho route %s ngày %s đã được xác nhận (status=%s), " +
                                "không thể tự động cập nhật lại. Vui lòng reset thủ công trước khi consolidate lại.",
                                route.getCode(), deliveryDate, existing.getStatus()),
                        HttpStatus.CONFLICT);
            }

            // 5c. Calculate totals from pre-computed line values on OrderItem
            BigDecimal totalVolume = BigDecimal.ZERO;
            BigDecimal totalWeight = BigDecimal.ZERO;
            for (Order order : routeOrders) {
                for (OrderItem item : order.getItems()) {
                    totalVolume = totalVolume.add(item.getLineVolumeM3());
                    totalWeight = totalWeight.add(item.getLineWeightKg());
                }
            }

            // 5d. Determine active/skipped stops (BR-06)
            Set<Long> storeIdsWithOrder = routeOrders.stream()
                    .map(o -> o.getStore().getId())
                    .collect(Collectors.toSet());

            int activeCount = 0;
            for (RouteStop rs : routeStops) {
                if (storeIdsWithOrder.contains(rs.getStore().getId())) {
                    activeCount++;
                }
            }
            int skippedCount = routeStops.size() - activeCount;

            // 5e. Upsert TripDraft
            TripDraft draft = (existing != null) ? existing : new TripDraft();
            draft.setRoute(route);
            draft.setDeliveryDate(deliveryDate);
            draft.setTotalVolumeM3(totalVolume);
            draft.setTotalWeightKg(totalWeight);
            draft.setActiveStopCount(activeCount);
            draft.setSkippedStopCount(skippedCount);
            draft.setStatus("DRAFT");
            tripDraftRepository.save(draft);

            // 5f. Refresh TripDraftStops (delete old, recreate — idempotent)
            tripDraftStopRepository.deleteByTripDraftId(draft.getId());
            tripDraftRepository.flush();

            for (RouteStop rs : routeStops) {
                boolean isActive = storeIdsWithOrder.contains(rs.getStore().getId());
                int orderCountAtStop = (int) routeOrders.stream()
                        .filter(o -> o.getStore().getId().equals(rs.getStore().getId()))
                        .count();

                TripDraftStop stop = TripDraftStop.builder()
                        .tripDraft(draft)
                        .routeStop(rs)
                        .store(rs.getStore())
                        .sequenceNo(rs.getSequenceOrder())
                        .isActive(isActive)
                        .orderCount(orderCountAtStop)
                        .build();
                tripDraftStopRepository.save(stop);
            }

            // 5g. Link orders back to this TripDraft
            List<Long> orderIds = routeOrders.stream().map(Order::getId).toList();
            orderRepository.updateTripDraftId(orderIds, draft.getId());

            planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                    draft.getId(), null, com.elog.entity.PlanningEventType.TRIP_DRAFT_CREATED,
                    com.elog.entity.PlanningActorType.SYSTEM, "System",
                    null, "DRAFT", "Gom đơn tạo/cập nhật Trip Draft " + draft.getId(),
                    null, null, null, null,
                    draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

            results.add(toResponse(draft, null));
        }

        return ConsolidateResponse.builder()
                .deliveryDate(deliveryDate)
                .tripDraftsCreatedOrUpdated(results.size())
                .tripDrafts(results)
                .skippedRoutes(skippedRoutes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<TripDraftResponse>> getTripDrafts(LocalDate deliveryDate, Pageable pageable) {
        Page<TripDraft> page = tripDraftRepository.findByDeliveryDate(deliveryDate, pageable);

        List<TripDraftResponse> data = page.getContent().stream()
                .map(td -> toResponse(td, null))
                .toList();

        return ApiResponse.<List<TripDraftResponse>>builder()
                .success(true)
                .data(data)
                .pagination(ApiResponse.PaginationInfo.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TripDraftResponse getTripDraftById(Long id) {
        TripDraft draft = tripDraftRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TRIP_DRAFT_NOT_FOUND,
                        "Trip Draft not found with id: " + id,
                        HttpStatus.NOT_FOUND));

        List<TripDraftStopResponse> stopResponses = draft.getStops().stream()
                .map(this::toStopResponse)
                .toList();

        return toResponse(draft, stopResponses);
    }

    // ── US-11: Stop Filtering & Manual Adjustment (TASK-02) ─────

    @Override
    @Transactional(readOnly = true)
    public TripDraftResponse getStopsForReview(Long tripDraftId) {
        // Reuse existing getTripDraftById — same logic, same response
        return getTripDraftById(tripDraftId);
    }

    @Override
    @Transactional
    public TripDraftStopResponse updateStop(Long tripDraftId, Long stopId, StopUpdateRequest request) {
        TripDraft draft = findDraftOrThrow(tripDraftId);

        // DC-07: block changes after PLANNED
        if (!"DRAFT".equals(draft.getStatus())) {
            throw new BusinessException(
                    ErrorCode.TRIP_DRAFT_LOCKED,
                    "Trip Draft already confirmed (status=" + draft.getStatus() + "). Stop adjustment not allowed.",
                    HttpStatus.CONFLICT);
        }

        TripDraftStop stop = tripDraftStopRepository.findById(stopId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "TripDraftStop not found with id: " + stopId,
                        HttpStatus.NOT_FOUND));

        // Verify stop belongs to this trip draft
        if (!stop.getTripDraft().getId().equals(tripDraftId)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Stop " + stopId + " does not belong to Trip Draft " + tripDraftId,
                    HttpStatus.NOT_FOUND);
        }

        // [ASSUMPTION]: Cannot activate a stop with no orders
        if (Boolean.TRUE.equals(request.getIsActive()) && stop.getOrderCount() == 0) {
            throw new BusinessException(
                    ErrorCode.CANNOT_ACTIVATE_EMPTY_STOP,
                    "Stop " + stop.getStore().getCode() + " has no orders for " + draft.getDeliveryDate()
                            + ". Cannot activate.",
                    HttpStatus.BAD_REQUEST);
        }

        // Update stop
        stop.setIsActive(request.getIsActive());
        stop.setOverrideNote(request.getOverrideNote());
        // Clear planned_eta when stop status changes (needs recalculation)
        stop.setPlannedEta(null);
        tripDraftStopRepository.save(stop);

        // Update counts on TripDraft
        int activeCount = tripDraftStopRepository.countByTripDraftIdAndIsActiveTrue(tripDraftId);
        int totalStops = draft.getStops().size();
        draft.setActiveStopCount(activeCount);
        draft.setSkippedStopCount(totalStops - activeCount);
        tripDraftRepository.save(draft);

        // Clear all planned_eta for active stops (force recalculation)
        List<TripDraftStop> activeStops = tripDraftStopRepository
                .findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(tripDraftId);
        for (TripDraftStop activeStop : activeStops) {
            activeStop.setPlannedEta(null);
        }
        tripDraftStopRepository.saveAll(activeStops);

        log.info("US-11: Stop {} (store={}) updated to isActive={} for TripDraft {}",
                stopId, stop.getStore().getCode(), request.getIsActive(), tripDraftId);

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, null, com.elog.entity.PlanningEventType.TRIP_DRAFT_UPDATED,
                com.elog.entity.PlanningActorType.USER, null,
                draft.getStatus(), draft.getStatus(), "Cập nhật điểm dừng " + stop.getStore().getCode() + " (isActive=" + request.getIsActive() + ")",
                null, "Stop ID: " + stop.getId(), null, stop.getStore().getCode(),
                draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

        return toStopResponse(stop);
    }

    // ── US-11: ETA Recalculation (TASK-03) ─────────────────────

    @Override
    @Transactional
    public RecalculateEtaResponse recalculateEta(Long tripDraftId, RecalculateEtaRequest request) {
        // Delegate to EtaCalculationService (Haversine implementation)
        List<StopEtaResponse> results = etaCalculationService
                .calculateAndPersist(tripDraftId, request.getPlannedDepartureTime());

        return RecalculateEtaResponse.builder()
                .tripDraftId(tripDraftId)
                .message("ETA recalculated for " + results.size() + " active stops.")
                .stops(results)
                .build();
    }

    // ── US-11: Dispatcher Confirm API — DC-07 Gate (TASK-04) ───

    @Override
    @Transactional
    public ConfirmResponse confirmTripDraft(Long tripDraftId, String currentUsername) {
        TripDraft draft = findDraftOrThrow(tripDraftId);

        // Pre-condition 1: status must be DRAFT
        if (!"DRAFT".equals(draft.getStatus())) {
            throw new BusinessException(
                    ErrorCode.ALREADY_CONFIRMED,
                    "Trip Draft " + tripDraftId + " is already in " + draft.getStatus() + " status.",
                    HttpStatus.CONFLICT);
        }

        // Pre-condition 2: must have ≥1 active stop
        int activeCount = tripDraftStopRepository.countByTripDraftIdAndIsActiveTrue(tripDraftId);
        if (activeCount == 0) {
            throw new BusinessException(
                    ErrorCode.NO_ACTIVE_STOP,
                    "Trip Draft has no active stops. At least 1 active stop required before confirmation.",
                    HttpStatus.BAD_REQUEST);
        }

        // Pre-condition 3: all active stops must have planned_eta
        int missingEtaCount = tripDraftStopRepository
                .countByTripDraftIdAndIsActiveTrueAndPlannedEtaIsNull(tripDraftId);
        if (missingEtaCount > 0) {
            throw new BusinessException(
                    ErrorCode.ETA_NOT_CALCULATED,
                    "All active stops must have ETA calculated before confirming. "
                            + missingEtaCount + " stops missing ETA. Call POST /recalculate-eta first.",
                    HttpStatus.BAD_REQUEST);
        }

        // Resolve current user
        User confirmer = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "User not found: " + currentUsername,
                        HttpStatus.NOT_FOUND));

        // Execute state transition: DRAFT → PLANNED
        LocalDateTime now = LocalDateTime.now();
        draft.setStatus("PLANNED");
        draft.setConfirmedAt(now);
        draft.setConfirmedBy(confirmer);
        tripDraftRepository.save(draft);

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, null, com.elog.entity.PlanningEventType.PLAN_CONFIRMED,
                com.elog.entity.PlanningActorType.USER, currentUsername,
                "DRAFT", "PLANNED", "Dispatcher xác nhận Trip Draft " + tripDraftId,
                null, null, null, null,
                draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

        log.info("US-11 DC-07: TripDraft id={} confirmed by {} at {}",
                tripDraftId, currentUsername, now);

        return ConfirmResponse.builder()
                .tripDraftId(tripDraftId)
                .fixedRouteCode(draft.getRoute().getCode())
                .deliveryDate(draft.getDeliveryDate())
                .status("PLANNED")
                .confirmedAt(now)
                .confirmedBy(ConfirmedByDto.builder()
                        .userId(confirmer.getId())
                        .fullName(confirmer.getFullName())
                        .build())
                .activeStopCount(activeCount)
                .summary("Trip Draft confirmed. Capacity validation (US-12) is now unlocked.")
                .build();
    }

    @Override
    @Transactional
    public void revertToDraft(Long tripDraftId, String currentUsername) {
        TripDraft draft = findDraftOrThrow(tripDraftId);

        // Guard: cannot revert if already assigned to a Trip
        if (tripRepository.existsByTripDraftId(tripDraftId)) {
            throw new BusinessException(
                    ErrorCode.TRIP_DRAFT_ALREADY_ASSIGNED,
                    "Trip Draft already assigned to a Trip. Cannot revert to DRAFT.",
                    HttpStatus.CONFLICT);
        }

        // Delete the generated Manifest associated with this TripDraft if any exists
        manifestRepository.findByTripDraftId(tripDraftId).ifPresent(m -> {
            manifestRepository.delete(m);
            log.info("US-11: Deleted manifest associated with TripDraft id={}", tripDraftId);
        });

        // Revert status to DRAFT
        draft.setStatus("DRAFT");
        draft.setConfirmedAt(null);
        draft.setConfirmedBy(null);
        draft.setValidatedAt(null);
        draft.setValidatedBy(null);
        draft.setVolumeCheckResult(ConstraintResult.NOT_CHECKED);
        draft.setWeightCheckResult(ConstraintResult.NOT_CHECKED);

        // Reset planned_eta of stops to null
        for (TripDraftStop stop : draft.getStops()) {
            stop.setPlannedEta(null);
        }

        tripDraftRepository.save(draft);

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, null, com.elog.entity.PlanningEventType.PLAN_REPLANNED,
                com.elog.entity.PlanningActorType.USER, currentUsername,
                "PLANNED", "DRAFT", "Hủy xác nhận, chuyển Trip Draft " + tripDraftId + " về DRAFT",
                null, null, null, null,
                draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));
        log.info("US-11: TripDraft id={} reverted to DRAFT status by {}", tripDraftId, currentUsername);
    }

    // ── Shared helpers ───────────────────────────────────────────

    private TripDraft findDraftOrThrow(Long id) {
        return tripDraftRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TRIP_DRAFT_NOT_FOUND,
                        "Trip Draft not found with id: " + id,
                        HttpStatus.NOT_FOUND));
    }

    // ── Mapping helpers ─────────────────────────────────────────

    private TripDraftResponse toResponse(TripDraft draft, List<TripDraftStopResponse> stops) {
        ConfirmedByDto confirmedByDto = null;
        if (draft.getConfirmedBy() != null) {
            confirmedByDto = ConfirmedByDto.builder()
                    .userId(draft.getConfirmedBy().getId())
                    .fullName(draft.getConfirmedBy().getFullName())
                    .build();
        }

        return TripDraftResponse.builder()
                .id(draft.getId())
                .routeId(draft.getRoute().getId())
                .routeCode(draft.getRoute().getCode())
                .deliveryDate(draft.getDeliveryDate())
                .totalVolumeM3(draft.getTotalVolumeM3())
                .totalWeightKg(draft.getTotalWeightKg())
                .activeStopCount(draft.getActiveStopCount())
                .skippedStopCount(draft.getSkippedStopCount())
                .status(draft.getStatus())
                .plannedDepartureTime(draft.getPlannedDepartureTime())
                .confirmedAt(draft.getConfirmedAt())
                .confirmedBy(confirmedByDto)
                .totalDistanceKm(draft.getTotalDistanceKm())
                .routePolyline(draft.getRoutePolyline())
                .stops(stops)
                .build();
    }
    private TripDraftStopResponse toStopResponse(TripDraftStop stop) {
        Long draftId = (stop.getTripDraft() != null) ? stop.getTripDraft().getId() : null;
        Long routeStopId = (stop.getRouteStop() != null) ? stop.getRouteStop().getId() : null;
        
        BigDecimal stopWeight = BigDecimal.ZERO;
        BigDecimal stopVolume = BigDecimal.ZERO;
        
        if (draftId != null && stop.getStore() != null) {
            List<OrderItem> items = orderItemRepository.findByStopForManifest(
                    stop.getStore().getId(), draftId);
            if (items != null) {
                stopWeight = items.stream()
                        .map(OrderItem::getLineWeightKg)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                stopVolume = items.stream()
                        .map(OrderItem::getLineVolumeM3)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }

        return TripDraftStopResponse.builder()
                .tripDraftStopId(stop.getId())
                .sequenceNo(stop.getSequenceNo())
                .storeId(stop.getStore() != null ? stop.getStore().getId() : null)
                .storeCode(stop.getStore() != null ? stop.getStore().getCode() : null)
                .storeName(stop.getStore() != null ? stop.getStore().getName() : null)
                .isActive(stop.getIsActive())
                .orderCount(stop.getOrderCount())
                .plannedEta(stop.getPlannedEta())
                .overrideNote(stop.getOverrideNote())
                .routeStopId(routeStopId)
                .stopVolumeM3(stopVolume)
                .stopWeightKg(stopWeight)
                .distanceFromPrevKm(stop.getDistanceFromPrevKm())
                .travelTimeFromPrevMin(stop.getTravelTimeFromPrevMin())
                .estimatedDistanceKm(stop.getDistanceFromPrevKm())
                .estimatedTravelMin(stop.getTravelTimeFromPrevMin())
                .build();

    }


    @Override
    @Transactional(readOnly = true)
    public List<StopOrderItemResponse> getStopOrderItems(Long tripDraftId, Long stopId) {
        TripDraftStop stop = tripDraftStopRepository.findById(stopId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "TripDraftStop not found with id: " + stopId,
                        HttpStatus.NOT_FOUND));

        if (!stop.getTripDraft().getId().equals(tripDraftId)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Stop " + stopId + " does not belong to Trip Draft " + tripDraftId,
                    HttpStatus.NOT_FOUND);
        }

        List<OrderItem> items = orderItemRepository.findByStopForManifest(
                stop.getStore().getId(), tripDraftId);

        if (items == null) {
            return Collections.emptyList();
        }

        return items.stream()
                .map(item -> StopOrderItemResponse.builder()
                        .orderId(item.getOrder().getId())
                        .orderRef(item.getOrder().getOrderRef())
                        .sku(item.getSku())
                        .productName(item.getProduct().getProductName())
                        .quantity(item.getQuantity())
                        .weightKg(item.getLineWeightKg())
                        .volumeM3(item.getLineVolumeM3())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public TripDraftResponse adjustDepartureTime(Long tripDraftId, com.elog.dto.request.AdjustDepartureTimeRequest request) {
        TripDraft draft = findDraftOrThrow(tripDraftId);
        if ("CONFIRMED".equals(draft.getStatus()) || "CANCELLED".equals(draft.getStatus())) {
            throw new BusinessException(
                    ErrorCode.TRIP_DRAFT_LOCKED,
                    "Trip Draft already confirmed or cancelled (status=" + draft.getStatus() + "). Adjustment not allowed.",
                    HttpStatus.CONFLICT);
        }
        draft.setPlannedDepartureTime(request.getNewDepartureTime());
        tripDraftRepository.save(draft);

        // Recalculate ETA for all stops
        recalculateEta(tripDraftId, new RecalculateEtaRequest(request.getNewDepartureTime()));

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, null, com.elog.entity.PlanningEventType.TRIP_DRAFT_UPDATED,
                com.elog.entity.PlanningActorType.USER, null,
                draft.getStatus(), draft.getStatus(), "Điều chỉnh giờ xuất phát thành " + request.getNewDepartureTime(),
                null, null, null, null,
                draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

        return getTripDraftById(tripDraftId);
    }

    @Override
    @Transactional
    public void settleDelay(Long tripDraftId, Long orderId, com.elog.dto.request.SettleDelayRequest request, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Order not found with id: " + orderId,
                        HttpStatus.NOT_FOUND));

        if (order.getTripDraft() == null || !order.getTripDraft().getId().equals(tripDraftId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Order " + orderId + " does not belong to Trip Draft " + tripDraftId,
                    HttpStatus.BAD_REQUEST);
        }

        TripDraft draft = order.getTripDraft();
        if ("CONFIRMED".equals(draft.getStatus()) || "CANCELLED".equals(draft.getStatus())) {
            throw new BusinessException(
                    ErrorCode.TRIP_DRAFT_LOCKED,
                    "Trip Draft already confirmed or cancelled (status=" + draft.getStatus() + "). Settlement not allowed.",
                    HttpStatus.CONFLICT);
        }

        User user = userRepository.findByUsername(username)
                .orElse(null);

        order.setIsDeliveryTimeOverridden(true);
        order.setTimeOverrideReason(request.getReason());
        order.setTimeOverrideAt(LocalDateTime.now());
        if (user != null) {
            order.setTimeOverrideBy(user.getId());
        }
        orderRepository.save(order);

        if (draft.getPlannedDepartureTime() != null) {
            recalculateEta(tripDraftId, new RecalculateEtaRequest(draft.getPlannedDepartureTime()));
        }

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, null, com.elog.entity.PlanningEventType.TRIP_DRAFT_UPDATED,
                com.elog.entity.PlanningActorType.USER, username,
                draft.getStatus(), draft.getStatus(), "Xử lý trễ đơn hàng " + order.getOrderRef() + ": " + request.getReason(),
                null, order.getOrderRef(), null, order.getStore() != null ? order.getStore().getCode() : null,
                draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

        log.info("Order id={} delay settled by user={}: {}", orderId, username, request.getReason());
    }

    @Override
    @Transactional
    public void excludeOrder(Long tripDraftId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Order not found with id: " + orderId,
                        HttpStatus.NOT_FOUND));

        if (order.getTripDraft() == null || !order.getTripDraft().getId().equals(tripDraftId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Order " + orderId + " does not belong to Trip Draft " + tripDraftId,
                    HttpStatus.BAD_REQUEST);
        }

        TripDraft draft = order.getTripDraft();
        if ("CONFIRMED".equals(draft.getStatus()) || "CANCELLED".equals(draft.getStatus())) {
            throw new BusinessException(
                    ErrorCode.TRIP_DRAFT_LOCKED,
                    "Trip Draft already confirmed or cancelled (status=" + draft.getStatus() + "). Order exclusion not allowed.",
                    HttpStatus.CONFLICT);
        }

        order.setTripDraft(null);
        order.setStatus("UNASSIGNED");
        orderRepository.save(order);

        // Recalculate draft weight, volume, and stop order counts / active states
        recalculateDraftTotals(draft);

        // Recalculate ETA for remaining active stops
        if (draft.getPlannedDepartureTime() != null) {
            recalculateEta(tripDraftId, new RecalculateEtaRequest(draft.getPlannedDepartureTime()));
        }

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, null, com.elog.entity.PlanningEventType.TRIP_DRAFT_UPDATED,
                com.elog.entity.PlanningActorType.USER, null,
                draft.getStatus(), draft.getStatus(), "Loại trừ đơn hàng " + order.getOrderRef() + " khỏi Trip Draft",
                null, order.getOrderRef(), null, order.getStore() != null ? order.getStore().getCode() : null,
                draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

        log.info("Order id={} excluded from TripDraft id={}", orderId, tripDraftId);
    }

    @Override
    @Transactional
    public void reIncludeOrder(Long tripDraftId, Long orderId) {
        TripDraft draft = findDraftOrThrow(tripDraftId);
        if ("CONFIRMED".equals(draft.getStatus()) || "CANCELLED".equals(draft.getStatus())) {
            throw new BusinessException(
                    ErrorCode.TRIP_DRAFT_LOCKED,
                    "Trip Draft already confirmed or cancelled (status=" + draft.getStatus() + "). Order re-inclusion not allowed.",
                    HttpStatus.CONFLICT);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Order not found with id: " + orderId,
                        HttpStatus.NOT_FOUND));

        order.setTripDraft(draft);
        order.setStatus("IMPORTED");
        orderRepository.save(order);

        // Recalculate draft weight, volume, and stop order counts / active states
        recalculateDraftTotals(draft);

        // Recalculate ETA for active stops
        if (draft.getPlannedDepartureTime() != null) {
            recalculateEta(tripDraftId, new RecalculateEtaRequest(draft.getPlannedDepartureTime()));
        }

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, null, com.elog.entity.PlanningEventType.TRIP_DRAFT_UPDATED,
                com.elog.entity.PlanningActorType.USER, null,
                draft.getStatus(), draft.getStatus(), "Thêm lại đơn hàng " + order.getOrderRef() + " vào Trip Draft",
                null, order.getOrderRef(), null, order.getStore() != null ? order.getStore().getCode() : null,
                draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

        log.info("Order id={} re-included into TripDraft id={}", orderId, tripDraftId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopOrderItemResponse> getExcludedOrders(Long tripDraftId) {
        TripDraft draft = findDraftOrThrow(tripDraftId);
        List<Long> storeIds = draft.getStops().stream()
                .map(stop -> stop.getStore().getId())
                .toList();

        if (storeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Order> excludedOrders = orderRepository.findExcludedOrdersByDeliveryDateAndStores(
                draft.getDeliveryDate(), storeIds);

        List<StopOrderItemResponse> response = new ArrayList<>();
        for (Order order : excludedOrders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    response.add(StopOrderItemResponse.builder()
                            .orderId(order.getId())
                            .orderRef(order.getOrderRef())
                            .sku(item.getSku())
                            .productName(item.getProduct() != null ? item.getProduct().getProductName() : null)
                            .quantity(item.getQuantity())
                            .weightKg(item.getLineWeightKg())
                            .volumeM3(item.getLineVolumeM3())
                            .build());
                }
            }
        }
        return response;
    }

    private void recalculateDraftTotals(TripDraft draft) {
        List<Order> remainingOrders = orderRepository.findByTripDraftId(draft.getId());
        BigDecimal totalVolume = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Order order : remainingOrders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            if (items != null) {
                for (OrderItem item : items) {
                    if (item.getLineVolumeM3() != null) {
                        totalVolume = totalVolume.add(item.getLineVolumeM3());
                    }
                    if (item.getLineWeightKg() != null) {
                        totalWeight = totalWeight.add(item.getLineWeightKg());
                    }
                }
            }
        }

        draft.setTotalVolumeM3(totalVolume);
        draft.setTotalWeightKg(totalWeight);

        // Recalculate stop orderCounts and active status
        List<TripDraftStop> stops = tripDraftStopRepository.findByTripDraftIdOrderBySequenceNoAsc(draft.getId());
        int activeCount = 0;
        int skippedCount = 0;

        for (TripDraftStop stop : stops) {
            long count = remainingOrders.stream()
                    .filter(o -> o.getStore().getId().equals(stop.getStore().getId()))
                    .count();
            stop.setOrderCount((int) count);
            boolean isActive = count > 0;
            stop.setIsActive(isActive);
            if (isActive) {
                activeCount++;
            } else {
                skippedCount++;
            }
        }
        tripDraftStopRepository.saveAll(stops);

        draft.setActiveStopCount(activeCount);
        draft.setSkippedStopCount(skippedCount);
        tripDraftRepository.save(draft);
    }
}
