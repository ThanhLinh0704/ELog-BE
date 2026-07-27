package com.elog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Response cho GET /api/trips/{id}/progress */
@Getter
@Builder
public class TripProgressResponse {

    private Long tripId;
    private String fixedRouteCode;
    private String deliveryDate;
    private String status;

    private VehicleInfo vehicle;
    private DriverInfo driver;

    private List<StopProgress> stops;

    private Object gpsLocation;
    private String gpsNote;

    @Getter
    @Builder
    public static class VehicleInfo {
        private String vehicleCode;
        private String plateNumber;
    }

    @Getter
    @Builder
    public static class DriverInfo {
        private Long userId;
        private String fullName;
        private String phone;
    }

    @Getter
    @Builder
    public static class StopProgress {
        private Long tripStopId;
        private Integer sequenceOrder;
        private String storeCode;
        private String storeName;
        private String status;

        private String plannedEta;
        private String actualArrivalTime;
        private String actualDepartureTime;
        private Long delayMinutes;

        private boolean hasException;
        private List<ExceptionDetail> exceptions;

        // Tọa độ cửa hàng — null nếu store chưa có tọa độ
        private Double latitude;
        private Double longitude;
    }

    @Getter
    @Builder
    public static class ExceptionDetail {
        private Long exceptionId;
        private String type;
        private String description;
        private String createdAt;
        private String resolvedAt;
    }
}
