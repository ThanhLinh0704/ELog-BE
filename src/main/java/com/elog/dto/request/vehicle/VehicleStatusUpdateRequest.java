package com.elog.dto.request.vehicle;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleStatusUpdateRequest {

    @NotNull(message = "FIELD_REQUIRED")
    private Boolean isActive;
}
