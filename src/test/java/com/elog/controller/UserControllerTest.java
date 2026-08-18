package com.elog.controller;

import com.elog.dto.request.user.UserCreateRequest;
import com.elog.dto.request.user.UserRolesUpdateRequest;
import com.elog.dto.request.user.UserStatusUpdateRequest;
import com.elog.dto.request.user.UserUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.user.UserResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass", List.of()));
    }

    @Test
    @DisplayName("L3-IDENTITY-005: GET /api/v1/users - List users returns 200 OK")
    void getAllUsers_Success() throws Exception {
        UserResponse user = UserResponse.builder().id(1L).username("admin").build();
        ApiResponse<List<UserResponse>> apiResponse = ApiResponse.<List<UserResponse>>builder().success(true).data(List.of(user)).build();
        when(userService.getAllUsers(any(), any(), any(), any(Pageable.class))).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IDENTITY-006: POST /api/v1/users - Create user returns 201 Created")
    void createUser_Success() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("driver01");
        request.setPassword("Password@123");
        request.setFullName("Nguyen Van A");
        request.setEmail("driver01@elog.com");
        request.setRoles(Set.of("ROLE_DRIVER"));

        UserResponse response = UserResponse.builder().id(1L).username("driver01").build();
        when(userService.createUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IDENTITY-007: GET /api/v1/users/{id} - Get user by ID returns 200 OK")
    void getUserById_Success() throws Exception {
        UserResponse response = UserResponse.builder().id(1L).username("admin").build();
        when(userService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IDENTITY-008: PUT /api/v1/users/{id} - Update user returns 200 OK")
    void updateUser_Success() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setFullName("Updated Admin");
        request.setEmail("admin@elog.com");

        UserResponse response = UserResponse.builder().id(1L).username("admin").build();
        when(userService.updateUser(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IDENTITY-009: PATCH /api/v1/users/{id}/roles - Update roles returns 200 OK")
    void updateUserRoles_Success() throws Exception {
        UserRolesUpdateRequest request = new UserRolesUpdateRequest();
        request.setRoles(Set.of("ROLE_ADMIN"));

        UserResponse response = UserResponse.builder().id(1L).username("admin").build();
        when(userService.updateUserRoles(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IDENTITY-010: PATCH /api/v1/users/{id}/status - Update status returns 200 OK")
    void updateUserStatus_Success() throws Exception {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setIsActive(false);

        UserResponse response = UserResponse.builder().id(1L).isActive(false).build();
        when(userService.updateUserStatus(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
