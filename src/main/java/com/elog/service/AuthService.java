package com.elog.service;

import com.elog.dto.request.LoginRequest;
import com.elog.dto.request.TokenRefreshRequest;
import com.elog.dto.response.TokenResponse;
import com.elog.dto.response.TokenRefreshResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);

    TokenRefreshResponse refresh(TokenRefreshRequest request);

    void logout(String refreshToken);
}