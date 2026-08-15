package com.elog.dto.response.exception;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExceptionListResponse {

    private String date;
    private int totalCount;
    private int unresolvedCount;
    private List<ExceptionItem> exceptions;

    @Getter
    @Builder
    public static class FilterInfo {
        private String type;
        private Object resolved;
    }

    @Getter
    @Builder
    public static class ExceptionItem {
        private Long exceptionId;
        private String exceptionType;
        private String rejectionType;

        // Trip / Route
        private Long tripId;
        private String fixedRouteCode;
        private String vehicleCode;
        private String driverName;

        // Stop
        private Long tripStopId;
        private String storeCode;
        private String storeName;

        // Timing
        private String plannedEta;
        private String actualArrivalTime;
        private Long delayMinutes;

        // Description & reporter
        private String description;
        private String reportedBy;
        private String createdAt;

        // Resolution
        private String resolvedAt;
        private String resolvedBy;
        private String resolutionNotes;
    }
}
