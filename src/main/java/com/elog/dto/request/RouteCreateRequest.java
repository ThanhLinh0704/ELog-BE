package com.elog.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteCreateRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 3, max = 20, message = "INVALID_SIZE")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "INVALID_FORMAT")
    private String code;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 2, max = 100, message = "INVALID_SIZE")
    private String name;

    @Size(max = 255, message = "INVALID_SIZE")
    private String description;
}
