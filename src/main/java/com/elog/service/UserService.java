package com.elog.service;

import com.elog.dto.request.user.UserCreateRequest;
import com.elog.dto.request.user.UserRolesUpdateRequest;
import com.elog.dto.request.user.UserStatusUpdateRequest;
import com.elog.dto.request.user.UserUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.user.UserResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);
    UserResponse getUserById(Long id);
    ApiResponse<List<UserResponse>> getAllUsers(String keyword, String role, Boolean isActive, Pageable pageable);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    UserResponse updateUserRoles(Long id, UserRolesUpdateRequest request, String currentUsername);
    UserResponse updateUserStatus(Long id, UserStatusUpdateRequest request, String currentUsername);
}
