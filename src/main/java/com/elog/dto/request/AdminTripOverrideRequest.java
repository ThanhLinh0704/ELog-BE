package com.elog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTripOverrideRequest {

    /**
     * Override action: "FORCE_RETURN" or "FORCE_COMPLETE_AND_RETURN"
     */
    @NotNull(message = "Hành động can thiệp (action) không được để trống")
    private String action;

    /**
     * Mandatory override reason / justification for audit log
     */
    @NotBlank(message = "Lý do can thiệp (reason) không được để trống")
    private String reason;

    /**
     * Optional status for remaining PENDING orders when action is "FORCE_COMPLETE_AND_RETURN".
     * Accepts "DELIVERED" or "FAILED" (defaults to "DELIVERED" if null/blank).
     */
    private String defaultPendingOrderStatus;
}
