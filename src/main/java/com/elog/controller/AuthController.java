package com.elog.controller;

import com.elog.dto.ApiResponse;
import com.elog.dto.LoginRequest;
import com.elog.dto.TokenRefreshRequest;
import com.elog.dto.TokenResponse;
import com.elog.dto.TokenRefreshResponse;
import com.elog.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return tokens")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully authenticated")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired access token using refresh token")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate refresh token")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request.getRefreshToken());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}