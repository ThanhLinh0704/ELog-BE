package com.elog.dto.request.user;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusUpdateRequest {
    @NotNull(message = "FIELD_REQUIRED")
    private Boolean isActive;
}
