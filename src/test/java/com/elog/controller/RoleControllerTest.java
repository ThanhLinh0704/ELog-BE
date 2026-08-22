package com.elog.controller;

import com.elog.dto.request.user.RolePermissionsUpdateRequest;
import com.elog.dto.response.user.PermissionResponse;
import com.elog.dto.response.user.RoleResponse;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("L3-IDENTITY-101: GET /api/v1/roles - Returns system role list with 200 OK")
    void getAllRoles_Success() throws Exception {
        RoleResponse role = RoleResponse.builder()
                .id(1L)
                .name("ADMIN")
                .permissions(Set.of())
                .build();
        when(roleService.getAllRoles()).thenReturn(List.of(role));

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("ADMIN"));
    }

    @Test
    @DisplayName("L3-IDENTITY-102: GET /api/v1/roles/{id} - Returns role detail with 200 OK")
    void getRoleById_Success() throws Exception {
        RoleResponse role = RoleResponse.builder()
                .id(1L)
                .name("ADMIN")
                .permissions(Set.of())
                .build();
        when(roleService.getRoleById(1L)).thenReturn(role);

        mockMvc.perform(get("/api/v1/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("ADMIN"));
    }

    @Test
    @DisplayName("L3-IDENTITY-103: PUT /api/v1/roles/{id}/permissions - Updates role permissions with 200 OK")
    void updateRolePermissions_Success() throws Exception {
        RolePermissionsUpdateRequest request = new RolePermissionsUpdateRequest();
        request.setPermissionIds(Set.of(1L, 2L));

        RoleResponse role = RoleResponse.builder()
                .id(1L)
                .name("ADMIN")
                .permissions(Set.of())
                .build();
        when(roleService.updateRolePermissions(eq(1L), any())).thenReturn(role);

        mockMvc.perform(put("/api/v1/roles/1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IDENTITY-100: GET /api/v1/permissions - Returns all permissions with 200 OK")
    void getAllPermissions_Success() throws Exception {
        PermissionResponse perm = PermissionResponse.builder()
                .id(1L)
                .name("user:read")
                .description("read user")
                .build();
        when(roleService.getAllPermissions()).thenReturn(List.of(perm));

        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("user:read"));
    }
}
