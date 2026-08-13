package com.elog.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiByRouteResponse {

    private KpiSummaryResponse.PeriodInfo period;
    private List<RouteKpi> routes;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteKpi {
        private String routeCode;
        private String routeName;
        private Integer totalTrips;
        private Double onTimeRatePct;
        private Double avgVolumeUtilPct;
        private Integer totalExceptions;
        private Integer totalRejections;
    }
}
