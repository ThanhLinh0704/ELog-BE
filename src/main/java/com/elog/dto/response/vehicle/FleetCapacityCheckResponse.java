package com.elog.dto.response.vehicle;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetCapacityCheckResponse {
    private LocalDate deliveryDate;
    private BigDecimal fleetTotalVolumeM3;
    private BigDecimal fleetTotalWeightKg;
    private BigDecimal dayTotalVolumeM3;
    private BigDecimal dayTotalWeightKg;
    private String volumeCheckResult;
    private String weightCheckResult;
    private boolean canDispatch;
    private String message;
}
