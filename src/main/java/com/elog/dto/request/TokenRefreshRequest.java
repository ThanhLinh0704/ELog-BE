package com.elog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenRefreshRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    private String refreshToken;
}