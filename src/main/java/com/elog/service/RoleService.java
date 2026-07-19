package com.elog.service;

import com.elog.dto.request.RolePermissionsUpdateRequest;
import com.elog.dto.response.PermissionResponse;
import com.elog.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {
    List<RoleResponse> getAllRoles();
    RoleResponse getRoleById(Long id);
    RoleResponse updateRolePermissions(Long id, RolePermissionsUpdateRequest request);
    List<PermissionResponse> getAllPermissions();
}
