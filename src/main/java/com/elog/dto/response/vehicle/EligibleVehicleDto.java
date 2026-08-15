package com.elog.dto.response.vehicle;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleVehicleDto {
    private Long vehicleId;
    private String plateNumber;
    private String vehicleType;
    private BigDecimal maxVolumeM3;
    private BigDecimal maxWeightKg;
    private BigDecimal remainingVolumeM3;
    private BigDecimal remainingWeightKg;
    private Long assignedDriverId;
    private String assignedDriverName;
    private Boolean assignedDriverAvailable;
    private String assignedDriverBusyReason;
}
