package com.elog.dto.response.kpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiByDriverResponse {

    private KpiSummaryResponse.PeriodInfo period;
    private List<DriverKpi> drivers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverKpi {
        private Long driverId;
        private String driverCode;
        private String fullName;
        private String phoneNumber;
        private Integer totalTrips;
        private Double totalDistanceKm;
        private Double onTimeRatePct;
        private Integer totalExceptions;
    }
}
