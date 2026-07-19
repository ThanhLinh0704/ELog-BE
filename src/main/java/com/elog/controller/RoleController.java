package com.elog.controller;

import com.elog.dto.request.RolePermissionsUpdateRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.PermissionResponse;
import com.elog.dto.response.RoleResponse;
import com.elog.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Roles & Permissions", description = "Role and Permission management APIs")
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/roles")
    @Operation(summary = "Get list of all roles with their assigned permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        List<RoleResponse> response = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/roles/{id}")
    @Operation(summary = "Get role detail with its assigned permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) {
        RoleResponse response = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/roles/{id}/permissions")
    @Operation(summary = "Update permissions for a specific role")
    @PreAuthorize("hasAuthority('role:write')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRolePermissions(
            @PathVariable Long id,
            @Valid @RequestBody RolePermissionsUpdateRequest request) {
        RoleResponse response = roleService.updateRolePermissions(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Role permissions updated successfully"));
    }

    @GetMapping("/permissions")
    @Operation(summary = "Get list of all available permissions in the system")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        List<PermissionResponse> response = roleService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
