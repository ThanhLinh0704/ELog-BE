package com.elog.service;

import com.elog.dto.request.UserCreateRequest;
import com.elog.dto.request.UserUpdateRequest;
import com.elog.dto.response.UserResponse;
import com.elog.entity.Role;
import com.elog.entity.User;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.UserMapper;
import com.elog.repository.RoleRepository;
import com.elog.repository.UserRepository;
import com.elog.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplReport5Test {
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserMapper userMapper;
    UserServiceImpl service;

    @BeforeEach void setUp() { service = new UserServiceImpl(userRepository, roleRepository, passwordEncoder, userMapper); }

    private UserCreateRequest createRequest() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("new_user"); request.setPassword("Test@1234"); request.setFullName("New User");
        request.setEmail("new@example.com"); request.setRoles(Set.of("DISPATCHER"));
        return request;
    }

    private void stubSuccessfulCreate(UserCreateRequest request) {
        Role role = Role.builder().id(2L).name("DISPATCHER").build();
        User entity = User.builder().username(request.getUsername()).fullName(request.getFullName())
                .email(request.getEmail()).roles(Set.of(role)).build();
        when(roleRepository.findByName("DISPATCHER")).thenReturn(Optional.of(role));
        when(userMapper.toEntity(request, Set.of(role))).thenReturn(entity);
        when(passwordEncoder.encode("Test@1234")).thenReturn("$2a$encoded");
        when(userRepository.save(entity)).thenAnswer(invocation -> { entity.setId(41L); return entity; });
        when(userMapper.toResponse(entity)).thenAnswer(invocation -> UserResponse.builder().id(entity.getId())
                .username(entity.getUsername()).roles(Set.of("DISPATCHER")).build());
    }

    @Test @DisplayName("[L1-US-01] valid user is saved with assigned role and encoded password")
    void validUserIsCreated() {
        UserCreateRequest request = createRequest(); stubSuccessfulCreate(request);
        UserResponse response = service.createUser(request);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertAll(() -> assertEquals(41L, response.getId()), () -> assertEquals("new_user", response.getUsername()),
                () -> assertEquals(Set.of("DISPATCHER"), response.getRoles()),
                () -> assertEquals("$2a$encoded", saved.getValue().getPasswordHash()));
        verify(passwordEncoder).encode("Test@1234");
    }

    @Test @DisplayName("[L1-US-02] plaintext password is never persisted")
    void plaintextPasswordIsNeverPersisted() {
        UserCreateRequest request = createRequest(); stubSuccessfulCreate(request);
        service.createUser(request);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertNotEquals("Test@1234", saved.getValue().getPasswordHash());
        assertEquals("$2a$encoded", saved.getValue().getPasswordHash());
    }

    @Test @DisplayName("[L1-US-03] duplicate username is rejected with conflict before save")
    void duplicateUsernameIsRejected() {
        UserCreateRequest request = createRequest();
        when(userRepository.existsByUsername("new_user")).thenReturn(true);
        BusinessException error = assertThrows(BusinessException.class, () -> service.createUser(request));
        assertAll(() -> assertEquals(ErrorCode.VALIDATION_FAILED, error.getErrorCode()),
                () -> assertEquals(HttpStatus.CONFLICT, error.getHttpStatus()));
        verify(userRepository, never()).save(any());
    }

    @Test @DisplayName("[L1-US-04] unknown role is rejected before user save")
    void unknownRoleIsRejected() {
        UserCreateRequest request = createRequest();
        when(roleRepository.findByName("DISPATCHER")).thenReturn(Optional.empty());
        BusinessException error = assertThrows(BusinessException.class, () -> service.createUser(request));
        assertEquals(ErrorCode.VALIDATION_FAILED, error.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test @DisplayName("[L1-US-05] duplicate email is rejected with conflict before save")
    void duplicateEmailIsRejected() {
        UserCreateRequest request = createRequest();
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);
        BusinessException error = assertThrows(BusinessException.class, () -> service.createUser(request));
        assertAll(() -> assertEquals(ErrorCode.VALIDATION_FAILED, error.getErrorCode()),
                () -> assertEquals(HttpStatus.CONFLICT, error.getHttpStatus()));
        verify(userRepository, never()).save(any());
    }

    private UserUpdateRequest updateRequest() {
        UserUpdateRequest request = new UserUpdateRequest(); request.setFullName("Updated Name"); request.setEmail("updated@example.com"); return request;
    }

    @Test @DisplayName("[L1-US-06] update saves full name and unique email")
    void updateSavesMutableFields() {
        User user = User.builder().id(9L).username("stable").fullName("Old").email("old@example.com").build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().id(9L).fullName("Updated Name").email("updated@example.com").build());
        service.updateUser(9L, updateRequest());
        assertAll(() -> assertEquals("Updated Name", user.getFullName()), () -> assertEquals("updated@example.com", user.getEmail()));
        verify(userRepository).save(user);
    }

    @Test @DisplayName("[L1-US-07] update leaves username immutable")
    void updateLeavesUsernameImmutable() {
        User user = User.builder().id(9L).username("stable").email("old@example.com").build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        service.updateUser(9L, updateRequest());
        assertEquals("stable", user.getUsername());
    }

    @Test @DisplayName("[L1-US-08] updating a missing user returns RESOURCE_NOT_FOUND")
    void missingUserIsRejected() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        BusinessException error = assertThrows(BusinessException.class, () -> service.updateUser(999L, updateRequest()));
        assertAll(() -> assertEquals(ErrorCode.RESOURCE_NOT_FOUND, error.getErrorCode()),
                () -> assertEquals(HttpStatus.NOT_FOUND, error.getHttpStatus()));
    }

    @Test @DisplayName("[L1-US-09] email owned by another user is rejected without save")
    void emailOwnedByAnotherUserIsRejected() {
        User user = User.builder().id(9L).username("stable").email("old@example.com").build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("updated@example.com")).thenReturn(true);
        BusinessException error = assertThrows(BusinessException.class, () -> service.updateUser(9L, updateRequest()));
        assertAll(() -> assertEquals(ErrorCode.VALIDATION_FAILED, error.getErrorCode()),
                () -> assertEquals(HttpStatus.CONFLICT, error.getHttpStatus()));
        verify(userRepository, never()).save(any());
    }
}
