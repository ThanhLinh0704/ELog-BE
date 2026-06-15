package com.elog.service.impl;

import com.elog.dto.*;
import com.elog.entity.Role;
import com.elog.entity.User;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.UserMapper;
import com.elog.repository.RoleRepository;
import com.elog.repository.UserRepository;
import com.elog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import com.elog.repository.specification.UserSpecification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Username already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Email already exists", HttpStatus.CONFLICT);
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid role: " + roleName,
                            HttpStatus.BAD_REQUEST));
            roles.add(role);
        }

        User user = userMapper.toEntity(request, roles);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found",
                        HttpStatus.NOT_FOUND));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<UserResponse>> getAllUsers(String keyword, String role, Boolean isActive,
            Pageable pageable) {
        // Kết hợp bộ lọc Specification động
        Specification<User> spec = Specification.where(UserSpecification.hasKeyword(keyword))
                .and(UserSpecification.hasRole(role))
                .and(UserSpecification.hasActiveStatus(isActive));

        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<UserResponse> content = userPage.getContent().stream()
                .map(userMapper::toResponse)
                .toList();

        ApiResponse.PaginationInfo pagination = ApiResponse.PaginationInfo.builder()
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();

        return ApiResponse.<List<UserResponse>>builder()
                .success(true)
                .data(content)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found",
                        HttpStatus.NOT_FOUND));

        // Kiểm tra trùng email với tài khoản khác
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Email already exists", HttpStatus.CONFLICT);
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserRoles(Long id, UserRolesUpdateRequest request, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found",
                        HttpStatus.NOT_FOUND));

        // TC-11: Admin không được tự gỡ role SYSTEM_ADMIN của chính mình
        if (user.getUsername().equals(currentUsername)) {
            boolean remainsAdmin = request.getRoles().contains("SYSTEM_ADMIN");
            if (!remainsAdmin) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, "Admin cannot remove their own SYSTEM_ADMIN role",
                        HttpStatus.FORBIDDEN);
            }
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Invalid role: " + roleName,
                            HttpStatus.BAD_REQUEST));
            roles.add(role);
        }

        user.setRoles(roles);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, UserStatusUpdateRequest request, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found",
                        HttpStatus.NOT_FOUND));

        // TC-10: Admin không được tự khóa tài khoản của chính mình
        if (user.getUsername().equals(currentUsername) && !request.getIsActive()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Admin cannot lock their own account",
                    HttpStatus.FORBIDDEN);
        }

        user.setIsActive(request.getIsActive());
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }
}
