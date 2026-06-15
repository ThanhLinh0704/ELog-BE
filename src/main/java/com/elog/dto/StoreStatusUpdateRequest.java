package com.elog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreStatusUpdateRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
