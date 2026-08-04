package com.elog.controller;

import com.elog.dto.request.LoginRequest;
import com.elog.dto.request.TokenRefreshRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.TokenRefreshResponse;
import com.elog.dto.response.TokenResponse;
import com.elog.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for login, refresh token, and logout")
public class AuthController {

    private final AuthService authService;
    private final com.elog.security.JwtUtils jwtUtils;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return tokens")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired access token using refresh token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate refresh/access token")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
            @RequestBody(required = false) TokenRefreshRequest request,
            HttpServletRequest httpServletRequest) {
            
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }

        String headerAuth = httpServletRequest.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            String jwt = headerAuth.substring(7);
            jwtUtils.blacklistToken(jwt);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}