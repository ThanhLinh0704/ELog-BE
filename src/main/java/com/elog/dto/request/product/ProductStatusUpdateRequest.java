package com.elog.dto.request.product;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatusUpdateRequest {

    @NotNull(message = "FIELD_REQUIRED")
    private Boolean isActive;
}
