package com.elog.dto.request.store;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreStatusUpdateRequest {

    @NotNull(message = "FIELD_REQUIRED")
    private Boolean isActive;
}
