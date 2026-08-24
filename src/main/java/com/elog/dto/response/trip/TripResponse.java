package com.elog.dto.response.trip;

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
public class TripResponse {
    private Long tripId;
    private Long tripDraftId;
    private Long executionId;
    private LocalDateTime returnedToWarehouseAt;
    private String fixedRouteCode;
    private LocalDate deliveryDate;
    private String status;
    private VehicleInfo vehicle;
    private DriverInfo driver;
    private BigDecimal totalWeightKg;
    private BigDecimal totalVolumeM3;
    private BigDecimal totalDistanceKm;
    private String routePolyline;
    private LocalTime plannedDepartureTime;

    private LocalDateTime lockedAt;
    private DriverInfo lockedBy;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    /** Số ngày quá hạn deliveryDate khi status vẫn DISPATCHED — null nếu chưa quá hạn. */
    private Integer daysOverdue;
    private Integer tripStopCount;
    private Long manifestId;
    private List<TripStopResponse> tripStops;
    private String handoverSlipUrl;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleInfo {
        private Long vehicleId;
        private String plateNumber;
        private String vehicleType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInfo {
        private Long userId;
        private String fullName;
    }
}
