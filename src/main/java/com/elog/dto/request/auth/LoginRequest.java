package com.elog.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    private String username;

    @NotBlank(message = "FIELD_REQUIRED")
    private String password;
}