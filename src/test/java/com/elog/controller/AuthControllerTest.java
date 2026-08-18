package com.elog.controller;

import com.elog.dto.request.auth.LoginRequest;
import com.elog.dto.request.auth.TokenRefreshRequest;
import com.elog.dto.response.auth.TokenRefreshResponse;
import com.elog.dto.response.auth.TokenResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.security.JwtUtils;
import com.elog.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("L3-AUTH-032: POST /api/v1/auth/login - Success returns 200 OK with TokenResponse")
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Dev@2025");

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("mock_access_token")
                .refreshToken("mock_refresh_token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .username("admin")
                .roles(List.of("ROLE_ADMIN"))
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"));
    }

    @Test
    @DisplayName("L3-AUTH-033: POST /api/v1/auth/refresh - Success returns 200 OK with new access token")
    void refresh_Success() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("valid_refresh_token");

        TokenRefreshResponse refreshResponse = TokenRefreshResponse.builder()
                .accessToken("new_access_token")
                .expiresIn(3600L)
                .build();

        when(authService.refresh(any(TokenRefreshRequest.class))).thenReturn(refreshResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new_access_token"));
    }

    @Test
    @DisplayName("L3-AUTH-034: POST /api/v1/auth/logout - Success revokes token and returns 200 OK")
    void logout_Success() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("valid_refresh_token");

        doNothing().when(authService).logout("valid_refresh_token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-AUTH-123: POST /api/v1/auth/login - Returns 401 on invalid credentials")
    void login_InvalidCredentials_Returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Tài khoản hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("L3-AUTH-124: POST /api/v1/auth/login - Returns 401 on disabled account")
    void login_DisabledAccount_Returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("locked_user");
        request.setPassword("Dev@2025");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Tài khoản đã bị vô hiệu hóa", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    @DisplayName("L3-AUTH-125: POST /api/v1/auth/refresh - Returns 401 on expired refresh token")
    void refresh_ExpiredToken_Returns401() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("expired_token");

        when(authService.refresh(any(TokenRefreshRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.TOKEN_EXPIRED, "Refresh token đã hết hạn", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("L3-AUTH-126: POST /api/v1/auth/refresh - Returns 401 on blacklisted token")
    void refresh_BlacklistedToken_Returns401() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("blacklisted_token");

        when(authService.refresh(any(TokenRefreshRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.TOKEN_INVALID, "Token không hợp lệ", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));
    }
}
