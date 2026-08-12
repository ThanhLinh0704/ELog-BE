package com.elog.service;

import com.elog.dto.request.auth.LoginRequest;
import com.elog.dto.request.auth.TokenRefreshRequest;
import com.elog.dto.response.auth.TokenRefreshResponse;
import com.elog.dto.response.auth.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);

    TokenRefreshResponse refresh(TokenRefreshRequest request);

    void logout(String refreshToken);
}
