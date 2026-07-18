package com.elog.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeliveryExceptionResponse {

    private Long exceptionId;
    private String exceptionType;
    private String rejectionType; // parse từ description prefix [TYPE] — null nếu TIME_EXCEPTION

    // Stop info
    private Long tripStopId;
    private String storeCode;
    private String storeName;
    private String tripStopStatus; // EXCEPTION

    // Trip info
    private Long tripId;
    private String fixedRouteCode;
    private String vehicleCode;

    // Timing
    private String plannedEta;
    private String actualArrivalTime;
    private Long delayMinutes;

    // Description & reporter
    private String description;
    private ReporterInfo reportedBy;
    private String createdAt;

    // Resolution
    private String resolvedAt;
    private ReporterInfo resolvedBy;
    private String resolutionNotes;

    // Optional message (dùng trong reject response)
    private String message;

    @Getter
    @Builder
    public static class ReporterInfo {
        private Long userId;
        private String fullName;
    }
}
