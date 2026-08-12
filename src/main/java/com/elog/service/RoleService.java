package com.elog.service;

import com.elog.dto.request.user.RolePermissionsUpdateRequest;
import com.elog.dto.response.user.PermissionResponse;
import com.elog.dto.response.user.RoleResponse;

import java.util.List;

public interface RoleService {
    List<RoleResponse> getAllRoles();
    RoleResponse getRoleById(Long id);
    RoleResponse updateRolePermissions(Long id, RolePermissionsUpdateRequest request);
    List<PermissionResponse> getAllPermissions();
}
