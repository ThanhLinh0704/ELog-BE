package com.elog.service;

import com.elog.dto.LoginRequest;
import com.elog.dto.TokenRefreshRequest;
import com.elog.dto.TokenResponse;
import com.elog.dto.TokenRefreshResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);

    TokenRefreshResponse refresh(TokenRefreshRequest request);

    void logout(String refreshToken);
}