package com.elog.dto.response.trip;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ActiveTripsResponse {

    private String date;
    private int totalActiveTrips;
    private List<TripSummary> trips;

    @Getter
    @Builder
    public static class TripSummary {
        private Long tripId;
        private String fixedRouteCode;
        private String vehicleCode;
        private String driverName;
        private String status;
        private String plannedDepartureTime;
        private String actualDepartureTime;

        private int totalStops;
        private int completedStops;
        private int pendingStops;
        private int exceptionStops;
        private int progressPercent;

        private boolean hasUnresolvedExceptions;
        private List<ExceptionSummary> exceptions;

        private Object gpsLocation;
        private String gpsNote;
    }

    @Getter
    @Builder
    public static class ExceptionSummary {
        private Long exceptionId;
        private String type;
        private String storeCode;
        private String description;
        private String resolvedAt;
    }
}
