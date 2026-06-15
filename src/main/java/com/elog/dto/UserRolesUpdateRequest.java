package com.elog.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class UserRolesUpdateRequest {
    @NotEmpty(message = "At least one role must be specified")
    private Set<String> roles;
}
