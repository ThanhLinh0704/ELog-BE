package com.elog.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleListItemResponse {

    private Long id;
    private String plateNumber;
    private String vehicleType;
    private BigDecimal maxWeightKg;
    private BigDecimal maxVolumeM3;
    private Boolean isActive;
}
