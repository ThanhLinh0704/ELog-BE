package com.elog.dto.response.kpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiByVehicleResponse {

    private KpiSummaryResponse.PeriodInfo period;
    private List<VehicleKpi> vehicles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleKpi {
        private Long vehicleId;
        private String licensePlate;
        private String vehicleType;
        private BigDecimal payloadKg;
        private BigDecimal maxVolumeM3;
        private Integer totalTrips;
        private Double totalDistanceKm;
        private Double avgVolumeUtilPct;
        private Double avgWeightUtilPct;
        private Double onTimeRatePct;
        private Integer totalExceptions;
    }
}
