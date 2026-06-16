package com.elog.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class UserRolesUpdateRequest {
    @NotEmpty(message = "FIELD_REQUIRED")
    private Set<String> roles;
}
