package com.elog.dto.response.vehicle;

import com.elog.entity.ConstraintResult;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IneligibleVehicleDto {
    private Long vehicleId;
    private String plateNumber;
    private String vehicleType;
    private BigDecimal maxVolumeM3;
    private BigDecimal maxWeightKg;
    private ConstraintResult volumeCheckResult;
    private ConstraintResult weightCheckResult;
    private String failureReason;
}
