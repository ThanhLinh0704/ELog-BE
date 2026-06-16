package com.elog.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class UserCreateRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Pattern(regexp = "^[A-Za-z0-9_]{3,50}$", 
             message = "USERNAME_INVALID")
    private String username;

    @NotBlank(message = "FIELD_REQUIRED")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "PASSWORD_INVALID")
    private String password;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 2, max = 100, message = "INVALID_SIZE")
    private String fullName;

    @NotBlank(message = "FIELD_REQUIRED")
    @Email(message = "INVALID_FORMAT")
    @Size(max = 100)
    private String email;

    @NotEmpty(message = "FIELD_REQUIRED")
    private Set<String> roles;
}
