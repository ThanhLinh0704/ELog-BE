package com.elog.service;

import com.elog.dto.request.LoginRequest;
import com.elog.dto.request.TokenRefreshRequest;
import com.elog.dto.response.TokenResponse;
import com.elog.entity.Permission;
import com.elog.entity.RefreshToken;
import com.elog.entity.Role;
import com.elog.entity.User;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.RefreshTokenRepository;
import com.elog.repository.UserRepository;
import com.elog.security.JwtUtils;
import com.elog.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplReport5Test {
    @Mock AuthenticationManager authenticationManager;
    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtUtils jwtUtils;
    @Mock PasswordEncoder passwordEncoder;
    AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(authenticationManager, userRepository, refreshTokenRepository, jwtUtils, passwordEncoder);
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 604_800_000L);
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Test@1234");
        return request;
    }

    private User activeUser(Set<Role> roles) {
        return User.builder().id(7L).username("admin").passwordHash("bcrypt-hash")
                .isActive(true).roles(roles).build();
    }

    private void stubSuccessfulLogin(User user) {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Test@1234", "bcrypt-hash")).thenReturn(true);
        when(jwtUtils.generateAccessToken(user)).thenReturn("access-token-xxx");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test @DisplayName("[L1-AU-01] valid login returns token pair and identity claims")
    void validLoginReturnsTokenPair() {
        Permission permission = Permission.builder().name("USER_READ").build();
        Role role = Role.builder().name("ADMIN").permissions(Set.of(permission)).build();
        User user = activeUser(Set.of(role));
        stubSuccessfulLogin(user);

        TokenResponse response = service.login(loginRequest());

        assertAll(
                () -> assertEquals("access-token-xxx", response.getAccessToken()),
                () -> assertNotNull(response.getRefreshToken()),
                () -> assertDoesNotThrow(() -> java.util.UUID.fromString(response.getRefreshToken())),
                () -> assertEquals("Bearer", response.getTokenType()),
                () -> assertEquals(7L, response.getUserId()),
                () -> assertEquals("admin", response.getUsername()),
                () -> assertEquals(Set.of("ADMIN"), new HashSet<>(response.getRoles())),
                () -> assertEquals(Set.of("USER_READ"), new HashSet<>(response.getPermissions())));
    }

    @Test @DisplayName("[L1-AU-02] ADMIN login returns role and ten flattened permissions")
    void adminLoginReturnsPermissions() {
        Set<Permission> permissions = new HashSet<>();
        IntStream.rangeClosed(1, 10).forEach(i -> permissions.add(Permission.builder().name("PERM_" + i).build()));
        User user = activeUser(Set.of(Role.builder().name("ADMIN").permissions(permissions).build()));
        stubSuccessfulLogin(user);

        TokenResponse response = service.login(loginRequest());

        assertEquals(Set.of("ADMIN"), new HashSet<>(response.getRoles()));
        assertEquals(10, response.getPermissions().size());
    }

    @Test @DisplayName("[L1-AU-03] login revokes old sessions before saving the new refresh token")
    void loginRevokesBeforeSavingRefreshToken() {
        User user = activeUser(Set.of());
        stubSuccessfulLogin(user);

        TokenResponse response = service.login(loginRequest());

        InOrder order = inOrder(refreshTokenRepository);
        order.verify(refreshTokenRepository).deleteByUser(user);
        order.verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertNotNull(response.getRefreshToken());
    }

    @Test @DisplayName("[L1-AU-04] unknown username is rejected without invoking authentication or tokens")
    void unknownUsernameIsRejected() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        BusinessException error = assertThrows(BusinessException.class, () -> service.login(loginRequest()));
        assertAll(
                () -> assertEquals(ErrorCode.INVALID_CREDENTIALS, error.getErrorCode()),
                () -> assertEquals(HttpStatus.UNAUTHORIZED, error.getHttpStatus()));
        verifyNoInteractions(authenticationManager, refreshTokenRepository, jwtUtils);
    }

    @Test @DisplayName("[L1-AU-05] incorrect password uses the same credential error without token side effects")
    void incorrectPasswordIsRejected() {
        User user = activeUser(Set.of());
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Test@1234", "bcrypt-hash")).thenReturn(false);
        BusinessException error = assertThrows(BusinessException.class, () -> service.login(loginRequest()));
        assertAll(
                () -> assertEquals(ErrorCode.INVALID_CREDENTIALS, error.getErrorCode()),
                () -> assertEquals(HttpStatus.UNAUTHORIZED, error.getHttpStatus()));
        verifyNoInteractions(authenticationManager, refreshTokenRepository, jwtUtils);
    }

    @Test @DisplayName("[L1-AU-06] inactive account is rejected with ACCOUNT_DISABLED")
    void inactiveAccountIsRejected() {
        User user = activeUser(Set.of());
        user.setIsActive(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        BusinessException error = assertThrows(BusinessException.class, () -> service.login(loginRequest()));
        assertAll(
                () -> assertEquals(ErrorCode.ACCOUNT_DISABLED, error.getErrorCode()),
                () -> assertEquals(HttpStatus.FORBIDDEN, error.getHttpStatus()));
    }

    @Test @DisplayName("[L1-AU-07] user without roles receives empty role and permission arrays")
    void noRolesReturnsEmptyClaims() {
        User user = activeUser(Set.of());
        stubSuccessfulLogin(user);
        TokenResponse response = service.login(loginRequest());
        assertAll(() -> assertTrue(response.getRoles().isEmpty()), () -> assertTrue(response.getPermissions().isEmpty()));
    }

    @Test @DisplayName("[L1-AU-13] logout deletes an existing refresh token")
    void logoutDeletesExistingToken() {
        RefreshToken token = RefreshToken.builder().token("refresh").build();
        when(refreshTokenRepository.findByToken("refresh")).thenReturn(Optional.of(token));
        service.logout("refresh");
        verify(refreshTokenRepository).delete(token);
    }

    @Test @DisplayName("[L1-AU-14] logout of an unknown token is idempotent")
    void logoutUnknownTokenIsIdempotent() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.logout("missing"));
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test @DisplayName("[L1-AU-15] unknown refresh token is rejected before access-token generation")
    void unknownRefreshTokenIsRejected() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("missing");
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());
        BusinessException error = assertThrows(BusinessException.class, () -> service.refresh(request));
        assertEquals(ErrorCode.TOKEN_INVALID, error.getErrorCode());
        verify(jwtUtils, never()).generateAccessToken(any());
    }

    @Test @DisplayName("[L1-AU-16] expired refresh token is deleted and rejected")
    void expiredRefreshTokenIsDeleted() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("expired");
        RefreshToken token = RefreshToken.builder().token("expired").expiryDate(Instant.now().minusSeconds(1)).build();
        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));
        BusinessException error = assertThrows(BusinessException.class, () -> service.refresh(request));
        assertEquals(ErrorCode.TOKEN_EXPIRED, error.getErrorCode());
        verify(refreshTokenRepository).delete(token);
    }

    @Test @DisplayName("[L1-AU-17] inactive refresh-token owner is denied without generating access token")
    void inactiveRefreshTokenOwnerIsDenied() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("locked");
        User user = activeUser(Set.of());
        user.setIsActive(false);
        RefreshToken token = RefreshToken.builder().token("locked").expiryDate(Instant.now().plusSeconds(60)).user(user).build();
        when(refreshTokenRepository.findByToken("locked")).thenReturn(Optional.of(token));
        BusinessException error = assertThrows(BusinessException.class, () -> service.refresh(request));
        assertEquals(ErrorCode.ACCESS_DENIED, error.getErrorCode());
        verify(jwtUtils, never()).generateAccessToken(any());
    }
}
