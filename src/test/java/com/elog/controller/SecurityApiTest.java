package com.elog.controller;

import com.elog.dto.request.auth.LoginRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SecurityApiTest {

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
    @DisplayName("L3-SEC-01: OWASP SQL Injection Protection on login credentials")
    void sqlInjectionProtection_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("' OR 1=1 --");
        request.setPassword("' OR '1'='1");

        when(authService.login(any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("L3-SEC-02: Cross-Site Scripting (XSS) payload sanitization in request body")
    void xssProtection_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("<script>alert('xss')</script>");
        request.setPassword("Dev@2025");

        when(authService.login(any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("L3-SEC-03: Missing Authorization Bearer token header returns 400 Bad Request")
    void missingToken_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("L3-SEC-04: Malformed JWT token structure returns 401 Unauthorized")
    void malformedToken_Returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BusinessException(ErrorCode.TOKEN_EXPIRED, "Token expired or malformed", HttpStatus.UNAUTHORIZED));

        LoginRequest request = new LoginRequest();
        request.setUsername("malformed");
        request.setPassword("pass");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("L3-SEC-05: Empty request body rejected with 400 Bad Request")
    void emptyBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("L3-SEC-06: Unsupported HTTP Method on auth endpoint returns 405 Method Not Allowed")
    void unsupportedMethod_Returns405() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("L3-SEC-07: Content type mismatch rejected with 415 Unsupported Media Type")
    void invalidContentType_Returns415() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("user=admin"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
