package com.elog.service.impl;

import com.elog.dto.request.UpdateOrderResultRequest;
import com.elog.dto.response.DriverTripResponse;
import com.elog.dto.response.DriverTripResponse.*;
import com.elog.dto.response.TripOutcomeResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.DriverTripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverTripServiceImpl implements DriverTripService {

    private final TripExecutionRepository tripExecutionRepo;
    private final DeliveryOrderResultRepository deliveryOrderResultRepo;
    private final TripOutcomeRepository tripOutcomeRepo;
    private final TripRepository tripRepo;
    private final OrderRepository orderRepo;
    private final TripDraftStopRepository tripDraftStopRepo;
    private final UserRepository userRepo;
    private final TripStopRepository tripStopRepo;
    private final DeliveryExceptionRepository deliveryExceptionRepo;
    private final VehicleRepository vehicleRepo;
    private final com.elog.service.TripOutcomeHistoryService tripOutcomeHistoryService;

    @Override
    @Transactional(readOnly = true)
    public DriverTripResponse getActiveTrip(String driverUsername) {
        LocalDate today = LocalDate.now();

        TripExecution execution = tripExecutionRepo
                .findByDriverUsernameAndStatusIn(driverUsername, List.of("IN_PROGRESS", "ASSIGNED"))
                .stream()
                .filter(te -> "IN_PROGRESS".equals(te.getStatus())
                        || (te.getTrip() != null && te.getTrip().getDeliveryDate() != null
                            && !te.getTrip().getDeliveryDate().isAfter(today)))
                .sorted(Comparator.comparing(te ->
                        te.getTrip() != null && te.getTrip().getDeliveryDate() != null
                                ? te.getTrip().getDeliveryDate()
                                : LocalDate.MAX))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chuyến xe đang phân công cho tài xế: " + driverUsername,
                        HttpStatus.NOT_FOUND));

        return buildDriverTripResponse(execution);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverTripResponse> getPendingReturnTrips(String driverUsername) {
        User driver = userRepo.findByUsername(driverUsername)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy tài xế", HttpStatus.NOT_FOUND));

        return tripExecutionRepo.findUnreturnedByDriverId(driver.getId()).stream()
                .filter(te -> List.of("COMPLETED", "COMPLETED_WITH_EXCEPTIONS").contains(te.getStatus()))
                .map(this::buildDriverTripResponse)
                .toList();
    }

    @Override
    @Transactional
    public DriverTripResponse startTrip(Long executionId, String driverUsername) {
        TripExecution execution = tripExecutionRepo.findById(executionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chuyến xe với ID: " + executionId,
                        HttpStatus.NOT_FOUND));

        verifyDriverAccess(execution, driverUsername);
        validateDeliveryDate(execution);

        if (!List.of("ASSIGNED", "DISPATCHED").contains(execution.getStatus())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Chuyến xe không ở trạng thái ASSIGNED (hiện tại: " + execution.getStatus() + ")",
                    HttpStatus.BAD_REQUEST);
        }

        String statusBefore = execution.getStatus();
        execution.setStatus("IN_PROGRESS");
        execution.setStartedAt(LocalDateTime.now());
        tripExecutionRepo.save(execution);

        Trip trip = execution.getTrip();
        if (trip != null && "ASSIGNED".equals(statusBefore)) {
            trip.setStatus(TripStatus.IN_PROGRESS);
            if (trip.getActualDepartureTime() == null) {
                trip.setActualDepartureTime(execution.getStartedAt());
            }
            if (trip.getVehicle() != null) {
                trip.getVehicle().setStatus(VehicleStatus.IN_USE);
            }
            tripRepo.save(trip);
        }

        tripOutcomeHistoryService.record(new com.elog.service.TripOutcomeHistoryService.OutcomeEventInput(
                executionId, trip != null ? trip.getTripId() : null,
                com.elog.entity.TripOutcomeEventType.START_TRIP,
                com.elog.entity.PlanningActorType.USER, driverUsername,
                statusBefore, "IN_PROGRESS",
                null, null, null, null, null, null, null, null,
                trip != null && trip.getRoute() != null ? trip.getRoute().getCode() : null,
                trip != null ? trip.getDeliveryDate() : null, driverUsername
        ));

        log.info("Driver {} started trip execution ID {}", driverUsername, executionId);
        return buildDriverTripResponse(execution);
    }

    @Override
    @Transactional
    public DriverTripResponse arriveAtStop(Long executionId, Long stopId, String driverUsername) {
        TripExecution execution = tripExecutionRepo.findById(executionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chuyến xe với ID: " + executionId,
                        HttpStatus.NOT_FOUND));

        verifyDriverAccess(execution, driverUsername);
        validateDeliveryDate(execution);

        if (!"IN_PROGRESS".equals(execution.getStatus())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Chỉ có thể bấm 'Đã đến điểm giao' khi chuyến xe ở trạng thái IN_PROGRESS",
                    HttpStatus.BAD_REQUEST);
        }

        Trip trip = execution.getTrip();
        if (trip == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Không tìm thấy Trip liên quan đến chuyến xe này", HttpStatus.NOT_FOUND);
        }

        List<TripStop> remainingStops = tripStopRepo.findRemainingStopsOrdered(trip.getTripId());
        if (remainingStops.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Tất cả các điểm dừng của chuyến xe này đã hoàn thành.", HttpStatus.BAD_REQUEST);
        }

        TripStop targetStop = tripStopRepo.findByTripDraftStopId(stopId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy điểm dừng với ID: " + stopId, HttpStatus.NOT_FOUND));

        if (!targetStop.getTrip().getTripId().equals(trip.getTripId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Điểm dừng không thuộc về chuyến xe này.", HttpStatus.BAD_REQUEST);
        }

        TripStop firstRemaining = remainingStops.get(0);
        if (!firstRemaining.getTripStopId().equals(targetStop.getTripStopId())) {
            throw new BusinessException(ErrorCode.PREVIOUS_STOP_NOT_DONE,
                    "Phải xử lý xong điểm dừng trước (thứ tự " + firstRemaining.getSequenceOrder() + ") mới được điểm tiếp theo.",
                    HttpStatus.BAD_REQUEST);
        }

        if (targetStop.getStatus() != TripStopStatus.PENDING) {
            throw new BusinessException(ErrorCode.STOP_NOT_PENDING,
                    "Điểm dừng này đã ở trạng thái " + targetStop.getStatus(), HttpStatus.BAD_REQUEST);
        }

        targetStop.setStatus(TripStopStatus.IN_PROGRESS);
        targetStop.setActualArrivalTime(LocalDateTime.now());
        tripStopRepo.save(targetStop);

        return buildDriverTripResponse(execution);
    }

    @Override
    @Transactional
    public DriverTripResponse updateOrderResult(Long executionId, Long orderId,
                                                UpdateOrderResultRequest request,
                                                String driverUsername) {
        TripExecution execution = tripExecutionRepo.findById(executionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chuyến xe với ID: " + executionId,
                        HttpStatus.NOT_FOUND));

        verifyDriverAccess(execution, driverUsername);
        validateDeliveryDate(execution);

        if (!"IN_PROGRESS".equals(execution.getStatus())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Chỉ có thể cập nhật đơn hàng khi chuyến xe ở trạng thái IN_PROGRESS",
                    HttpStatus.BAD_REQUEST);
        }

        String newStatus = request.getStatus().toUpperCase();
        if (List.of("FAILED", "PARTIALLY_DELIVERED").contains(newStatus)) {
            if (request.getReasonCode() == null || request.getReasonCode().isBlank()) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "Bắt buộc nhập lý do (reasonCode) khi giao hàng thất bại hoặc giao một phần",
                        HttpStatus.BAD_REQUEST);
            }
        }

        DeliveryOrderResult result = deliveryOrderResultRepo
                .findByTripExecutionIdAndOrderId(executionId, orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đơn hàng ID " + orderId + " trong chuyến xe này",
                        HttpStatus.NOT_FOUND));

        String orderStatusBefore = result.getStatus();
        TripDraftStop stop = result.getStop();
        List<DeliveryOrderResult> stopResultsBefore = stop != null
                ? deliveryOrderResultRepo.findByTripExecutionId(executionId).stream()
                .filter(r -> r.getStop() != null && r.getStop().getId().equals(stop.getId()))
                .toList()
                : List.of();
        String stopStatusBefore = aggregateStopStatusFromResults(stopResultsBefore);

        result.setStatus(newStatus);
        result.setReasonCode(request.getReasonCode());
        result.setExceptionText(request.getExceptionText());
        result.setUpdatedAt(LocalDateTime.now());
        deliveryOrderResultRepo.save(result);

        Trip trip = execution.getTrip();
        Order order = result.getOrder();
        com.elog.entity.TripOutcomeEventType eventType = "DELIVERED".equals(newStatus)
                ? com.elog.entity.TripOutcomeEventType.ORDER_DELIVERED
                : "FAILED".equals(newStatus)
                ? com.elog.entity.TripOutcomeEventType.ORDER_FAILED
                : com.elog.entity.TripOutcomeEventType.ORDER_PARTIALLY_DELIVERED;

        tripOutcomeHistoryService.record(new com.elog.service.TripOutcomeHistoryService.OutcomeEventInput(
                executionId, trip != null ? trip.getTripId() : null,
                eventType, com.elog.entity.PlanningActorType.USER, driverUsername,
                orderStatusBefore, newStatus,
                orderId, order != null ? order.getOrderRef() : null,
                stop != null ? stop.getId() : null,
                stop != null && stop.getStore() != null ? stop.getStore().getCode() : null,
                newStatus, request.getReasonCode(), request.getExceptionText(), null,
                trip != null && trip.getRoute() != null ? trip.getRoute().getCode() : null,
                trip != null ? trip.getDeliveryDate() : null, driverUsername
        ));

        if (stop != null) {
            List<DeliveryOrderResult> stopResultsAfter = deliveryOrderResultRepo.findByTripExecutionId(executionId).stream()
                    .filter(r -> r.getStop() != null && r.getStop().getId().equals(stop.getId()))
                    .toList();
            String stopStatusAfter = aggregateStopStatusFromResults(stopResultsAfter);

            if (!stopStatusBefore.equals(stopStatusAfter)) {
                tripOutcomeHistoryService.record(new com.elog.service.TripOutcomeHistoryService.OutcomeEventInput(
                        executionId, trip != null ? trip.getTripId() : null,
                        com.elog.entity.TripOutcomeEventType.STOP_STATUS_CHANGED,
                        com.elog.entity.PlanningActorType.SYSTEM, null,
                        stopStatusBefore, stopStatusAfter,
                        null, null, stop.getId(),
                        stop.getStore() != null ? stop.getStore().getCode() : null,
                        null, null, null, null,
                        trip != null && trip.getRoute() != null ? trip.getRoute().getCode() : null,
                        trip != null ? trip.getDeliveryDate() : null, driverUsername
                ));

                // Sync to System A TripStop entity if exists
                tripStopRepo.findByTripDraftStopId(stop.getId()).ifPresent(ts -> {
                    LocalDateTime now = LocalDateTime.now();
                    if (ts.getActualArrivalTime() == null) {
                        ts.setActualArrivalTime(now);
                    }
                    ts.setActualDepartureTime(now);

                    if ("DELIVERED".equals(stopStatusAfter)) {
                        ts.setStatus(TripStopStatus.COMPLETED);
                    } else if ("FAILED".equals(stopStatusAfter) || "PARTIALLY_DELIVERED".equals(stopStatusAfter)) {
                        ts.setStatus(TripStopStatus.EXCEPTION);
                    }
                    tripStopRepo.save(ts);
                });
            }
        }

        // Record log and DeliveryException if failed or partial
        if (List.of("FAILED", "PARTIALLY_DELIVERED").contains(newStatus)) {
            log.warn("Order ID {} updated to exception status {} by driver {}", orderId, newStatus, driverUsername);

            if (!deliveryExceptionRepo.existsByOrderIdAndExceptionTypeAndResolvedAtIsNull(orderId, ExceptionType.DELIVERY_REJECTION)) {
                User driver = userRepo.findByUsername(driverUsername).orElse(null);
                Long driverId = driver != null ? driver.getId() : null;
                String reasonCode = request.getReasonCode() != null ? request.getReasonCode() : newStatus;
                String userDesc = request.getExceptionText();
                String fullDesc = (userDesc != null && !userDesc.isBlank())
                        ? "[" + reasonCode + "] " + userDesc.trim()
                        : "[" + reasonCode + "]";

                DeliveryException ex = DeliveryException.builder()
                        .tripExecutionId(executionId)
                        .orderId(orderId)
                        .exceptionType(ExceptionType.DELIVERY_REJECTION)
                        .reportedBy(driverId != null ? driverId : 1L)
                        .description(fullDesc)
                        .build();
                deliveryExceptionRepo.save(ex);
                log.info("Created DeliveryException id={} for FT-09 executionId={} orderId={}",
                        ex.getExceptionId(), executionId, orderId);
            }
        }

        log.info("Driver {} updated order ID {} status to {}", driverUsername, orderId, newStatus);
        return buildDriverTripResponse(execution);
    }

    @Override
    @Transactional
    public TripOutcomeResponse completeTrip(Long executionId, String driverUsername) {
        TripExecution execution = tripExecutionRepo.findById(executionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chuyến xe với ID: " + executionId,
                        HttpStatus.NOT_FOUND));

        verifyDriverAccess(execution, driverUsername);
        validateDeliveryDate(execution);

        if (!"IN_PROGRESS".equals(execution.getStatus())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Chuyến xe không ở trạng thái IN_PROGRESS",
                    HttpStatus.BAD_REQUEST);
        }

        // Guard: check all orders are terminal (not PENDING)
        long pendingCount = deliveryOrderResultRepo.countByTripExecutionIdAndStatus(executionId, "PENDING");
        if (pendingCount > 0) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Còn " + pendingCount + " đơn hàng ở trạng thái PENDING. Phải cập nhật kết quả cho tất cả đơn trước khi hoàn tất chuyến.",
                    HttpStatus.BAD_REQUEST);
        }

        long totalCount = deliveryOrderResultRepo.countByTripExecutionId(executionId);
        long deliveredCount = deliveryOrderResultRepo.countByTripExecutionIdAndStatus(executionId, "DELIVERED");
        long failedCount = deliveryOrderResultRepo.countByTripExecutionIdAndStatus(executionId, "FAILED");
        long partialCount = deliveryOrderResultRepo.countByTripExecutionIdAndStatus(executionId, "PARTIALLY_DELIVERED");

        boolean hasExceptions = (failedCount > 0 || partialCount > 0);
        String finalExecutionStatus = hasExceptions ? "COMPLETED_WITH_EXCEPTIONS" : "COMPLETED";

        String statusBefore = execution.getStatus();
        execution.setStatus(finalExecutionStatus);
        execution.setCompletedAt(LocalDateTime.now());
        tripExecutionRepo.save(execution);

        Trip trip = execution.getTrip();
        if (trip != null) {
            trip.setStatus(TripStatus.COMPLETED);
            if (trip.getCompletedAt() == null) {
                trip.setCompletedAt(execution.getCompletedAt());
            }
            tripRepo.save(trip);
        }

        tripOutcomeHistoryService.record(new com.elog.service.TripOutcomeHistoryService.OutcomeEventInput(
                executionId, trip != null ? trip.getTripId() : null,
                com.elog.entity.TripOutcomeEventType.COMPLETE_TRIP,
                com.elog.entity.PlanningActorType.USER, driverUsername,
                statusBefore, finalExecutionStatus,
                null, null, null, null, null, null, null, null,
                trip != null && trip.getRoute() != null ? trip.getRoute().getCode() : null,
                trip != null ? trip.getDeliveryDate() : null, driverUsername
        ));

        // Create Submitted Trip Outcome for Dispatcher validation
        TripOutcome outcome = TripOutcome.builder()
                .tripExecution(execution)
                .status("SUBMITTED")
                .totalOrders((int) totalCount)
                .deliveredCount((int) deliveredCount)
                .failedCount((int) failedCount)
                .partialCount((int) partialCount)
                .submittedAt(LocalDateTime.now())
                .version(1)
                .build();
        tripOutcomeRepo.save(outcome);

        tripOutcomeHistoryService.record(new com.elog.service.TripOutcomeHistoryService.OutcomeEventInput(
                executionId, trip != null ? trip.getTripId() : null,
                com.elog.entity.TripOutcomeEventType.OUTCOME_SUBMITTED,
                com.elog.entity.PlanningActorType.SYSTEM, null,
                "IN_PROGRESS", "SUBMITTED",
                null, null, null, null, null, null, null, null,
                trip != null && trip.getRoute() != null ? trip.getRoute().getCode() : null,
                trip != null ? trip.getDeliveryDate() : null, driverUsername
        ));

        log.info("Driver {} completed trip execution ID {} with status {}", driverUsername, executionId, finalExecutionStatus);

        String tripCodeStr = trip != null ? "TRIP-" + trip.getTripId() : null;
        return TripOutcomeResponse.builder()
                .id(outcome.getId())
                .executionId(execution.getId())
                .tripId(trip != null ? trip.getTripId() : null)
                .tripCode(tripCodeStr)
                .driverName(execution.getDriver() != null ? execution.getDriver().getFullName() : null)
                .vehiclePlate(trip != null && trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .status(outcome.getStatus())
                .totalOrders(outcome.getTotalOrders())
                .deliveredCount(outcome.getDeliveredCount())
                .failedCount(outcome.getFailedCount())
                .partialCount(outcome.getPartialCount())
                .submittedAt(outcome.getSubmittedAt())
                .version(outcome.getVersion())
                .build();
    }

    @Override
    @Transactional
    public DriverTripResponse returnToWarehouse(Long executionId, String driverUsername) {
        TripExecution execution = tripExecutionRepo.findById(executionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chuyến xe với ID: " + executionId,
                        HttpStatus.NOT_FOUND));

        verifyDriverAccess(execution, driverUsername);

        if (!List.of("COMPLETED", "COMPLETED_WITH_EXCEPTIONS").contains(execution.getStatus())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Chuyến xe chưa hoàn thành (trạng thái hiện tại: " + execution.getStatus() + "). Không thể xác nhận về kho.",
                    HttpStatus.BAD_REQUEST);
        }

        if (execution.getReturnedToWarehouseAt() != null) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Chuyến xe đã được xác nhận về kho trước đó lúc: " + execution.getReturnedToWarehouseAt(),
                    HttpStatus.BAD_REQUEST);
        }

        execution.setReturnedToWarehouseAt(LocalDateTime.now());
        tripExecutionRepo.save(execution);

        // Update vehicle status back to AVAILABLE
        Trip trip = execution.getTrip();
        if (trip != null && trip.getVehicle() != null) {
            trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        }

        log.info("Driver {} confirmed return to warehouse for trip execution ID {}. Vehicle released to AVAILABLE.", driverUsername, executionId);
        return buildDriverTripResponse(execution);
    }

    private void verifyDriverAccess(TripExecution execution, String driverUsername) {
        if (execution.getDriver() == null || !driverUsername.equals(execution.getDriver().getUsername())) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED_ACCESS,
                    "Bạn không được phép thao tác trên chuyến xe của tài xế khác",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void validateDeliveryDate(TripExecution execution) {
        if (execution.getTrip() != null && execution.getTrip().getDeliveryDate() != null) {
            LocalDate deliveryDate = execution.getTrip().getDeliveryDate();
            if (deliveryDate.isAfter(LocalDate.now())) {
                String formattedDate = deliveryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                throw new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "Chưa đến ngày giao hàng (ngày giao: " + formattedDate + "). Không thể thực hiện chuyến xe trước ngày giao.",
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    private DriverTripResponse buildDriverTripResponse(TripExecution execution) {
        Trip trip = execution.getTrip();
        Long tripDraftId = trip != null && trip.getTripDraft() != null ? trip.getTripDraft().getId() : null;

        List<TripStop> myTripStops = trip != null
                ? tripStopRepo.findByTripTripIdOrderBySequenceOrderAsc(trip.getTripId())
                : List.of();

        List<TripDraftStop> stops;
        if (!myTripStops.isEmpty()) {
            stops = myTripStops.stream()
                    .map(TripStop::getTripDraftStop)
                    .filter(Objects::nonNull)
                    .filter(ds -> Boolean.TRUE.equals(ds.getIsActive()))
                    .toList();
        } else {
            stops = tripDraftId != null
                    ? tripDraftStopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(tripDraftId)
                    : List.of();
        }

        Set<Long> myStoreIds = stops.stream()
                .map(s -> s.getStore() != null ? s.getStore().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Order> orders = (tripDraftId != null && !myStoreIds.isEmpty())
                ? orderRepo.findByTripDraftId(tripDraftId).stream()
                        .filter(o -> o.getStore() != null && myStoreIds.contains(o.getStore().getId()))
                        .toList()
                : List.of();

        List<DeliveryOrderResult> results = deliveryOrderResultRepo.findByTripExecutionId(execution.getId());
        Map<Long, DeliveryOrderResult> resultMap = results.stream()
                .collect(Collectors.toMap(r -> r.getOrder().getId(), r -> r, (a, b) -> a));

        // Group orders by store ID
        Map<Long, List<Order>> ordersByStore = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getStore().getId()));

        List<DriverStopDto> stopDtos = new ArrayList<>();
        List<LifoLoadingItemDto> lifoGuidance = new ArrayList<>();

        int totalOrdersCount = orders.size();
        int completedCount = 0;
        int pendingCount = 0;

        Map<Long, TripStop> tripStopMap = (trip != null)
                ? tripStopRepo.findByTripTripIdOrderBySequenceOrderAsc(trip.getTripId()).stream()
                        .collect(Collectors.toMap(ts -> ts.getTripDraftStop().getId(), ts -> ts, (a, b) -> a))
                : Map.of();

        // Build Stop DTOs in forward delivery sequence
        for (TripDraftStop stop : stops) {
            Long storeId = stop.getStore().getId();
            List<Order> storeOrders = ordersByStore.getOrDefault(storeId, List.of());

            List<DriverOrderDto> orderDtos = new ArrayList<>();
            for (Order o : storeOrders) {
                DeliveryOrderResult r = resultMap.get(o.getId());
                String orderStatus = r != null ? r.getStatus() : "PENDING";

                if ("PENDING".equals(orderStatus)) pendingCount++;
                else completedCount++;

                List<DriverOrderItemDto> itemDtos = o.getItems().stream()
                        .map(i -> DriverOrderItemDto.builder()
                                .sku(i.getSku())
                                .productName(i.getProduct() != null ? i.getProduct().getProductName() : null)
                                .quantity(i.getQuantity())
                                .unitWeightKg(i.getUnitWeightKg())
                                .unitVolumeM3(i.getUnitVolumeM3())
                                .build())
                        .toList();

                orderDtos.add(DriverOrderDto.builder()
                        .orderId(o.getId())
                        .orderRef(o.getOrderRef())
                        .recipientName(o.getRecipientName())
                        .recipientPhone(o.getRecipientPhone())
                        .deliveryTimeWindow(o.getDeliveryTimeWindow())
                        .notes(o.getNotes())
                        .deliveryStatus(orderStatus)
                        .items(itemDtos)
                        .build());
            }

            // Aggregate stop status
            String stopStatus = aggregateStopStatus(orderDtos);

            TripStop ts = tripStopMap.get(stop.getId());
            String arrivalStatus = (ts != null && ts.getStatus() != TripStopStatus.PENDING) ? "ARRIVED" : "PENDING";
            LocalDateTime actualArrivalTime = ts != null ? ts.getActualArrivalTime() : null;

            stopDtos.add(DriverStopDto.builder()
                    .stopId(stop.getId())
                    .sequenceNo(stop.getSequenceNo())
                    .storeCode(stop.getStore().getCode())
                    .storeName(stop.getStore().getName())
                    .address(stop.getStore().getAddressDetail())
                    .plannedEta(stop.getPlannedEta() != null ? stop.getPlannedEta().toLocalTime() : null)
                    .closingTime(stop.getStore().getTimeWindowEnd())
                    .aggregatedStatus(stopStatus)
                    .arrivalStatus(arrivalStatus)
                    .actualArrivalTime(actualArrivalTime)
                    .orders(orderDtos)
                    .build());
        }

        // Build LIFO guidance (REVERSE sequence of delivery stops: load last stop first at the bottom/back)
        List<TripDraftStop> reverseStops = new ArrayList<>(stops);
        Collections.reverse(reverseStops);

        int loadingRank = 1;
        for (TripDraftStop stop : reverseStops) {
            Long storeId = stop.getStore().getId();
            List<Order> storeOrders = ordersByStore.getOrDefault(storeId, List.of());
            for (Order o : storeOrders) {
                for (OrderItem item : o.getItems()) {
                    String instruction = String.format("Xếp vị trí #%d (Giao tại điểm dừng #%d: %s)",
                            loadingRank, stop.getSequenceNo(), stop.getStore().getName());

                    lifoGuidance.add(LifoLoadingItemDto.builder()
                            .loadingOrder(loadingRank++)
                            .stopSequenceNo(stop.getSequenceNo())
                            .storeName(stop.getStore().getName())
                            .orderRef(o.getOrderRef())
                            .sku(item.getSku())
                            .productName(item.getProduct() != null ? item.getProduct().getProductName() : null)
                            .quantity(item.getQuantity())
                            .instruction(instruction)
                            .build());
                }
            }
        }

        String tripCodeStr = trip != null ? "TRIP-" + trip.getTripId() : null;

        return DriverTripResponse.builder()
                .executionId(execution.getId())
                .tripId(trip != null ? trip.getTripId() : null)
                .tripCode(tripCodeStr)
                .deliveryDate(trip != null ? trip.getDeliveryDate() : null)
                .status(execution.getStatus())
                .assignmentVersion(execution.getAssignmentVersion())
                .returnedToWarehouseAt(execution.getReturnedToWarehouseAt())
                .vehicleCode(trip != null && trip.getVehicle() != null ? trip.getVehicle().getVehicleCode() : null)
                .plateNumber(trip != null && trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .driverName(execution.getDriver() != null ? execution.getDriver().getFullName() : null)
                .stops(stopDtos)
                .lifoLoadingGuidance(lifoGuidance)
                .totalStops(stops.size())
                .totalOrders(totalOrdersCount)
                .completedOrdersCount(completedCount)
                .pendingOrdersCount(pendingCount)
                .build();
    }

    private String aggregateStopStatus(List<DriverOrderDto> orders) {
        if (orders.isEmpty()) return "PENDING";
        boolean anyPending = orders.stream().anyMatch(o -> "PENDING".equals(o.getDeliveryStatus()));
        if (anyPending) return "PENDING";
        boolean allDelivered = orders.stream().allMatch(o -> "DELIVERED".equals(o.getDeliveryStatus()));
        if (allDelivered) return "DELIVERED";
        boolean anyFailed = orders.stream().anyMatch(o -> "FAILED".equals(o.getDeliveryStatus()));
        if (anyFailed) return "FAILED";
        return "PARTIALLY_DELIVERED";
    }

    private String aggregateStopStatusFromResults(List<DeliveryOrderResult> results) {
        if (results.isEmpty()) return "PENDING";
        boolean anyPending = results.stream().anyMatch(o -> "PENDING".equals(o.getStatus()));
        if (anyPending) return "PENDING";
        boolean allDelivered = results.stream().allMatch(o -> "DELIVERED".equals(o.getStatus()));
        if (allDelivered) return "DELIVERED";
        boolean anyFailed = results.stream().anyMatch(o -> "FAILED".equals(o.getStatus()));
        if (anyFailed) return "FAILED";
        return "PARTIALLY_DELIVERED";
    }

    @Override
    @Transactional
    public DriverTripResponse adminOverrideTripExecution(Long executionId, com.elog.dto.request.AdminTripOverrideRequest request, String adminUsername) {
        TripExecution execution = tripExecutionRepo.findById(executionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chuyến xe với ID: " + executionId,
                        HttpStatus.NOT_FOUND));

        String action = request.getAction().toUpperCase();
        String reason = request.getReason().trim();

        if ("FORCE_RETURN".equals(action)) {
            if (!List.of("COMPLETED", "COMPLETED_WITH_EXCEPTIONS").contains(execution.getStatus())) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "Chỉ có thể Force Return khi chuyến xe đã ở trạng thái hoàn thành (COMPLETED hoặc COMPLETED_WITH_EXCEPTIONS). Trạng thái hiện tại: " + execution.getStatus(),
                        HttpStatus.BAD_REQUEST);
            }
            if (execution.getReturnedToWarehouseAt() != null) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "Chuyến xe đã được xác nhận về kho trước đó lúc: " + execution.getReturnedToWarehouseAt(),
                        HttpStatus.BAD_REQUEST);
            }

            execution.setReturnedToWarehouseAt(LocalDateTime.now());
            tripExecutionRepo.save(execution);

            Trip trip = execution.getTrip();
            if (trip != null && trip.getVehicle() != null) {
                trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
                vehicleRepo.save(trip.getVehicle());
            }

            tripOutcomeHistoryService.record(new com.elog.service.TripOutcomeHistoryService.OutcomeEventInput(
                    executionId, trip != null ? trip.getTripId() : null,
                    TripOutcomeEventType.ADMIN_OVERRIDE_FORCE_RETURN,
                    com.elog.entity.PlanningActorType.USER, adminUsername,
                    execution.getStatus(), execution.getStatus(),
                    null, null, null, null, null, reason, null, null,
                    trip != null && trip.getRoute() != null ? trip.getRoute().getCode() : null,
                    trip != null ? trip.getDeliveryDate() : null,
                    execution.getDriver() != null ? execution.getDriver().getUsername() : null
            ));

            log.info("Admin {} performed FORCE_RETURN on trip execution ID {}. Reason: {}", adminUsername, executionId, reason);
            return buildDriverTripResponse(execution);

        } else if ("FORCE_COMPLETE_AND_RETURN".equals(action)) {
            if (List.of("COMPLETED", "COMPLETED_WITH_EXCEPTIONS").contains(execution.getStatus()) && execution.getReturnedToWarehouseAt() != null) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "Chuyến xe đã hoàn tất và đã về kho từ trước.",
                        HttpStatus.BAD_REQUEST);
            }

            // Update remaining PENDING orders
            String defaultOrderStatus = (request.getDefaultPendingOrderStatus() != null && !request.getDefaultPendingOrderStatus().isBlank())
                    ? request.getDefaultPendingOrderStatus().toUpperCase()
                    : "DELIVERED";

            List<DeliveryOrderResult> pendingResults = deliveryOrderResultRepo.findByTripExecutionId(executionId).stream()
                    .filter(r -> "PENDING".equals(r.getStatus()))
                    .toList();

            for (DeliveryOrderResult r : pendingResults) {
                r.setStatus(defaultOrderStatus);
                r.setReasonCode(reason);
                r.setExceptionText("[ADMIN_OVERRIDE] " + reason);
                r.setUpdatedAt(LocalDateTime.now());
                deliveryOrderResultRepo.save(r);
            }

            long totalCount = deliveryOrderResultRepo.countByTripExecutionId(executionId);
            long deliveredCount = deliveryOrderResultRepo.countByTripExecutionIdAndStatus(executionId, "DELIVERED");
            long failedCount = deliveryOrderResultRepo.countByTripExecutionIdAndStatus(executionId, "FAILED");
            long partialCount = deliveryOrderResultRepo.countByTripExecutionIdAndStatus(executionId, "PARTIALLY_DELIVERED");

            boolean hasExceptions = (failedCount > 0 || partialCount > 0);
            String finalExecutionStatus = hasExceptions ? "COMPLETED_WITH_EXCEPTIONS" : "COMPLETED";

            String statusBefore = execution.getStatus();
            execution.setStatus(finalExecutionStatus);
            if (execution.getStartedAt() == null) {
                execution.setStartedAt(LocalDateTime.now());
            }
            if (execution.getCompletedAt() == null) {
                execution.setCompletedAt(LocalDateTime.now());
            }
            execution.setReturnedToWarehouseAt(LocalDateTime.now());
            tripExecutionRepo.save(execution);

            Trip trip = execution.getTrip();
            if (trip != null) {
                trip.setStatus(TripStatus.COMPLETED);
                if (trip.getCompletedAt() == null) {
                    trip.setCompletedAt(execution.getCompletedAt());
                }
                if (trip.getVehicle() != null) {
                    trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
                    vehicleRepo.save(trip.getVehicle());
                }
                tripRepo.save(trip);
            }

            // Update or create TripOutcome
            TripOutcome outcome = tripOutcomeRepo.findByTripExecutionId(executionId).orElse(null);
            if (outcome == null) {
                outcome = TripOutcome.builder()
                        .tripExecution(execution)
                        .status("SUBMITTED")
                        .totalOrders((int) totalCount)
                        .deliveredCount((int) deliveredCount)
                        .failedCount((int) failedCount)
                        .partialCount((int) partialCount)
                        .submittedAt(LocalDateTime.now())
                        .version(1)
                        .build();
            } else {
                outcome.setTotalOrders((int) totalCount);
                outcome.setDeliveredCount((int) deliveredCount);
                outcome.setFailedCount((int) failedCount);
                outcome.setPartialCount((int) partialCount);
            }
            tripOutcomeRepo.save(outcome);

            tripOutcomeHistoryService.record(new com.elog.service.TripOutcomeHistoryService.OutcomeEventInput(
                    executionId, trip != null ? trip.getTripId() : null,
                    TripOutcomeEventType.ADMIN_OVERRIDE_FORCE_COMPLETE,
                    com.elog.entity.PlanningActorType.USER, adminUsername,
                    statusBefore, finalExecutionStatus,
                    null, null, null, null, null, reason, null, null,
                    trip != null && trip.getRoute() != null ? trip.getRoute().getCode() : null,
                    trip != null ? trip.getDeliveryDate() : null,
                    execution.getDriver() != null ? execution.getDriver().getUsername() : null
            ));

            log.info("Admin {} performed FORCE_COMPLETE_AND_RETURN on trip execution ID {}. Reason: {}", adminUsername, executionId, reason);
            return buildDriverTripResponse(execution);

        } else {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Action không hợp lệ: " + request.getAction() + ". Chỉ chấp nhận FORCE_RETURN hoặc FORCE_COMPLETE_AND_RETURN.",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
