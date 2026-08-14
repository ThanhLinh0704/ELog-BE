package com.elog.service;

import com.elog.dto.request.RolePermissionsUpdateRequest;
import com.elog.dto.response.PermissionResponse;
import com.elog.dto.response.RoleResponse;
import com.elog.entity.Permission;
import com.elog.entity.Role;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.PermissionRepository;
import com.elog.repository.RoleRepository;
import com.elog.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplReport5Test {
    @Mock RoleRepository roleRepository; @Mock PermissionRepository permissionRepository;
    RoleServiceImpl service;
    @BeforeEach void setUp() { service = new RoleServiceImpl(roleRepository, permissionRepository); }
    private Permission permission(long id) { return Permission.builder().id(id).name("PERM_" + id).description("desc " + id).build(); }
    private Role role(long id, String name, Set<Permission> permissions) { return Role.builder().id(id).name(name).permissions(permissions).build(); }

    @Test @DisplayName("[L1-RL-01] all roles are returned with IDs names and permissions")
    void allRolesAreMapped() {
        when(roleRepository.findAll()).thenReturn(List.of(role(1,"ADMIN",Set.of(permission(1))), role(2,"MANAGER",Set.of(permission(2))), role(3,"DISPATCHER",Set.of(permission(3))), role(4,"DRIVER",Set.of(permission(4)))));
        List<RoleResponse> result = service.getAllRoles();
        assertEquals(4, result.size());
        result.forEach(r -> assertAll(() -> assertNotNull(r.getId()), () -> assertNotNull(r.getName()), () -> assertEquals(1, r.getPermissions().size())));
    }

    @Test @DisplayName("[L1-RL-02] role without permissions maps to an empty set")
    void roleWithoutPermissionsIsEmpty() {
        when(roleRepository.findAll()).thenReturn(List.of(role(4,"DRIVER",Set.of())));
        assertTrue(service.getAllRoles().getFirst().getPermissions().isEmpty());
    }

    @Test @DisplayName("[L1-RL-03] role found by ID is fully mapped")
    void roleByIdIsMapped() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1,"ADMIN",Set.of(permission(10)))));
        RoleResponse response = service.getRoleById(1L);
        assertAll(() -> assertEquals(1L,response.getId()), () -> assertEquals("ADMIN",response.getName()), () -> assertEquals(Set.of("PERM_10"), response.getPermissions().stream().map(PermissionResponse::getName).collect(java.util.stream.Collectors.toSet())));
    }

    @Test @DisplayName("[L1-RL-04] missing role lookup returns RESOURCE_NOT_FOUND")
    void missingRoleLookupIsRejected() {
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());
        BusinessException error=assertThrows(BusinessException.class,()->service.getRoleById(999L));
        assertAll(()->assertEquals(ErrorCode.RESOURCE_NOT_FOUND,error.getErrorCode()),()->assertEquals(HttpStatus.NOT_FOUND,error.getHttpStatus()));
    }

    @Test @DisplayName("[L1-RL-05] valid permission IDs replace and save the role permission set")
    void validPermissionsAreSaved() {
        Role role=role(1,"ADMIN",Set.of()); when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        Set<Long> ids=Set.of(10L,11L,12L); ids.forEach(id->when(permissionRepository.findById(id)).thenReturn(Optional.of(permission(id))));
        when(roleRepository.save(role)).thenReturn(role);
        RoleResponse response=service.updateRolePermissions(1L,RolePermissionsUpdateRequest.builder().permissionIds(ids).build());
        assertEquals(ids,role.getPermissions().stream().map(Permission::getId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(3,response.getPermissions().size()); verify(roleRepository).save(role);
    }

    @Test @DisplayName("[L1-RL-06] permission update rejects a missing role")
    void permissionUpdateRejectsMissingRole() {
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());
        BusinessException error=assertThrows(BusinessException.class,()->service.updateRolePermissions(999L,RolePermissionsUpdateRequest.builder().permissionIds(Set.of(1L)).build()));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,error.getErrorCode());
    }

    @Test @DisplayName("[L1-RL-07] missing permission rejects the update without saving the role")
    void missingPermissionRejectsUpdate() {
        Role role=role(1,"ADMIN",Set.of()); when(roleRepository.findById(1L)).thenReturn(Optional.of(role)); when(permissionRepository.findById(999L)).thenReturn(Optional.empty());
        BusinessException error=assertThrows(BusinessException.class,()->service.updateRolePermissions(1L,RolePermissionsUpdateRequest.builder().permissionIds(Set.of(999L)).build()));
        assertAll(()->assertEquals(ErrorCode.RESOURCE_NOT_FOUND,error.getErrorCode()),()->assertEquals(HttpStatus.NOT_FOUND,error.getHttpStatus())); verify(roleRepository,never()).save(any());
    }

    @Test @DisplayName("[L1-RL-08] all system permissions are returned with ID code and description")
    void allPermissionsAreMapped() {
        List<Permission> permissions=LongStream.rangeClosed(1,15).mapToObj(this::permission).toList(); when(permissionRepository.findAll()).thenReturn(permissions);
        List<PermissionResponse> result=service.getAllPermissions(); assertEquals(15,result.size());
        result.forEach(p->assertAll(()->assertNotNull(p.getId()),()->assertTrue(p.getName().startsWith("PERM_")),()->assertTrue(p.getDescription().startsWith("desc "))));
    }
}
