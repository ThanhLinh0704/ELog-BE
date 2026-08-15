package com.elog.dto.request.route;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteStatusUpdateRequest {

    @NotNull(message = "FIELD_REQUIRED")
    private Boolean isActive;
}
