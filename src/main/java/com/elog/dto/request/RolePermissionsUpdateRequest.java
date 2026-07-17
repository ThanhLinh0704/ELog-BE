package com.elog.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionsUpdateRequest {

    @NotNull(message = "Permission IDs set cannot be null")
    private Set<Long> permissionIds;
}
