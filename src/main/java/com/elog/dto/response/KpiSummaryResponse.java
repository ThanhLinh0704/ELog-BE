package com.elog.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiSummaryResponse {

    private PeriodInfo period;
    private LocalDateTime generatedAt;
    private OnTimeDeliveryKpi onTimeDelivery;
    private FleetUtilizationKpi fleetUtilization;
    private TripCompletionKpi tripCompletion;
    private ExceptionKpi exceptions;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodInfo {
        private LocalDate startDate;
        private LocalDate endDate;
        private String preset;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnTimeDeliveryKpi {
        private Double rate;
        private Integer onTimeStops;
        private Integer totalProcessedStops;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FleetUtilizationKpi {
        private Double avgVolumeUtilizationPct;
        private Double avgWeightUtilizationPct;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripCompletionKpi {
        private Double rate;
        private Integer completedTrips;
        private Integer totalTrips;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExceptionKpi {
        private Double exceptionRate;
        private Integer totalExceptions;
        private Integer totalTimeExceptions;
        private Integer totalRejections;
        private Integer unresolvedCount;
    }
}
