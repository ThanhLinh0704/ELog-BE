package com.elog.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class UserCreateRequest {

    @NotBlank(message = "Username cannot be blank")
    @Pattern(regexp = "^[A-Za-z0-9_]{3,50}$", 
             message = "Username must be 3-50 characters and contain only letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "Password must be at least 8 characters, containing at least 1 uppercase letter, 1 number, and 1 special character")
    private String password;

    @NotBlank(message = "Full name cannot be blank")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @NotEmpty(message = "At least one role must be assigned")
    private Set<String> roles;
}
