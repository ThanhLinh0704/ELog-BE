package com.elog.service.impl;

import com.elog.dto.request.user.RolePermissionsUpdateRequest;
import com.elog.dto.response.user.PermissionResponse;
import com.elog.dto.response.user.RoleResponse;
import com.elog.entity.Permission;
import com.elog.entity.Role;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.PermissionRepository;
import com.elog.repository.RoleRepository;
import com.elog.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found with id: " + id, HttpStatus.NOT_FOUND));
        return toRoleResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse updateRolePermissions(Long id, RolePermissionsUpdateRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found with id: " + id, HttpStatus.NOT_FOUND));

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            for (Long permId : request.getPermissionIds()) {
                Permission permission = permissionRepository.findById(permId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Permission not found with id: " + permId, HttpStatus.NOT_FOUND));
                permissions.add(permission);
            }
        }

        role.setPermissions(permissions);
        Role savedRole = roleRepository.save(role);
        return toRoleResponse(savedRole);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        if (permission == null) return null;
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }

    private RoleResponse toRoleResponse(Role role) {
        if (role == null) return null;
        Set<PermissionResponse> permissions = role.getPermissions().stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toSet());
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .permissions(permissions)
                .build();
    }
}
