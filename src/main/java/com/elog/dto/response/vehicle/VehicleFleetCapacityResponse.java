package com.elog.dto.response.vehicle;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleFleetCapacityResponse {

    private long activeVehicleCount;
    private BigDecimal totalMaxWeightKg;
    private BigDecimal totalMaxVolumeM3;
}
