package com.elog.dto.response;

import com.elog.entity.ConstraintResult;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityValidationResultResponse {
    private Long tripDraftId;
    private String fixedRouteCode;
    private String deliveryDate;
    private String newStatus;
    private BigDecimal totalVolumeM3;
    private BigDecimal totalWeightKg;
    private boolean validationPassed;
    private ConstraintResult volumeCheckResult;
    private ConstraintResult weightCheckResult;
    private List<EligibleVehicleDto> eligibleVehicles;
    private List<IneligibleVehicleDto> ineligibleVehicles;
    private String bindingConstraint;
    private String suggestion;
    private LocalDateTime validatedAt;
    private ConfirmedByDto validatedBy;
    private String message;
}
