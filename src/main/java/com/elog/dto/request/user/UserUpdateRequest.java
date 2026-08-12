package com.elog.dto.request.user;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 2, max = 100, message = "INVALID_SIZE")
    private String fullName;

    @NotBlank(message = "FIELD_REQUIRED")
    @Email(message = "INVALID_FORMAT")
    @Size(max = 100)
    private String email;
}
