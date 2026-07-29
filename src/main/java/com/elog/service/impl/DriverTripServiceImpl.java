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

import java.time.LocalDateTime;
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

    @Override
    @Transactional(readOnly = true)
    public DriverTripResponse getActiveTrip(String driverUsername) {
        TripExecution execution = tripExecutionRepo.findActiveByDriverUsername(driverUsername)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chuyến xe đang phân công cho tài xế: " + driverUsername,
                        HttpStatus.NOT_FOUND));

        return buildDriverTripResponse(execution);
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

        if (!"ASSIGNED".equals(execution.getStatus())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Chuyến xe không ở trạng thái ASSIGNED (hiện tại: " + execution.getStatus() + ")",
                    HttpStatus.BAD_REQUEST);
        }

        execution.setStatus("IN_PROGRESS");
        execution.setStartedAt(LocalDateTime.now());
        tripExecutionRepo.save(execution);

        log.info("Driver {} started trip execution ID {}", driverUsername, executionId);
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

        result.setStatus(newStatus);
        result.setUpdatedAt(LocalDateTime.now());
        deliveryOrderResultRepo.save(result);

        // Record log if failed or partial
        if (List.of("FAILED", "PARTIALLY_DELIVERED").contains(newStatus)) {
            log.warn("Order ID {} updated to exception status {} by driver {}", orderId, newStatus, driverUsername);
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

        execution.setStatus(finalExecutionStatus);
        execution.setCompletedAt(LocalDateTime.now());
        tripExecutionRepo.save(execution);

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

        log.info("Driver {} completed trip execution ID {} with status {}", driverUsername, executionId, finalExecutionStatus);

        Trip trip = execution.getTrip();
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

    private DriverTripResponse buildDriverTripResponse(TripExecution execution) {
        Trip trip = execution.getTrip();
        Long tripDraftId = trip != null && trip.getTripDraft() != null ? trip.getTripDraft().getId() : null;

        List<TripDraftStop> stops = tripDraftId != null
                ? tripDraftStopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(tripDraftId)
                : List.of();

        List<Order> orders = tripDraftId != null
                ? orderRepo.findByTripDraftId(tripDraftId)
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

            stopDtos.add(DriverStopDto.builder()
                    .stopId(stop.getId())
                    .sequenceNo(stop.getSequenceNo())
                    .storeCode(stop.getStore().getCode())
                    .storeName(stop.getStore().getName())
                    .address(stop.getStore().getAddressDetail())
                    .plannedEta(stop.getPlannedEta() != null ? stop.getPlannedEta().toLocalTime() : null)
                    .closingTime(stop.getStore().getTimeWindowEnd())
                    .aggregatedStatus(stopStatus)
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
        boolean allDelivered = orders.stream().allMatch(o -> "DELIVERED".equals(o.getDeliveryStatus()));
        if (allDelivered) return "DELIVERED";
        boolean anyFailed = orders.stream().anyMatch(o -> "FAILED".equals(o.getDeliveryStatus()));
        if (anyFailed) return "FAILED";
        boolean anyPartial = orders.stream().anyMatch(o -> "PARTIALLY_DELIVERED".equals(o.getDeliveryStatus()));
        if (anyPartial) return "PARTIALLY_DELIVERED";
        return "PENDING";
    }
}
