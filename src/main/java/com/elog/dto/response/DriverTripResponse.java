package com.elog.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverTripResponse {

    private Long executionId;
    private Long tripId;
    private String tripCode;
    private LocalDate deliveryDate;
    private String status; // ASSIGNED, IN_PROGRESS, COMPLETED, etc.
    private Integer assignmentVersion;
    private LocalDateTime returnedToWarehouseAt;

    // Vehicle info
    private String vehicleCode;
    private String plateNumber;

    // Driver info
    private String driverName;

    // Stops & Orders with LIFO sequence guidance
    private List<DriverStopDto> stops;
    private List<LifoLoadingItemDto> lifoLoadingGuidance;

    // Summary counts
    private Integer totalStops;
    private Integer totalOrders;
    private Integer completedOrdersCount;
    private Integer pendingOrdersCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverStopDto {
        private Long stopId;
        private Integer sequenceNo;
        private String storeCode;
        private String storeName;
        private String address;
        private LocalTime plannedEta;
        private LocalTime closingTime;
        private String aggregatedStatus; // PENDING, DELIVERED, PARTIAL, FAILED
        private String arrivalStatus; // PENDING, ARRIVED
        private LocalDateTime actualArrivalTime;
        private List<DriverOrderDto> orders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverOrderDto {
        private Long orderId;
        private String orderRef;
        private String recipientName;
        private String recipientPhone;
        private String deliveryTimeWindow;
        private String notes;
        private String deliveryStatus; // PENDING, DELIVERED, PARTIALLY_DELIVERED, FAILED, CANCELLED
        private String exceptionReason;
        private List<DriverOrderItemDto> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverOrderItemDto {
        private String sku;
        private String productName;
        private Integer quantity;
        private BigDecimal unitWeightKg;
        private BigDecimal unitVolumeM3;
    }

    /**
     * LIFO Loading Guidance — item to load first is delivered LAST.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LifoLoadingItemDto {
        private Integer loadingOrder; // 1 = Load first (bottom/back of truck)
        private Integer stopSequenceNo;
        private String storeName;
        private String orderRef;
        private String sku;
        private String productName;
        private Integer quantity;
        private String instruction; // e.g. "Xếp vào trong cùng (Giao tại điểm dừng cuối)"
    }
}
