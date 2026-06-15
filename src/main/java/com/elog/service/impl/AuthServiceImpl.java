package com.elog.service.impl;

import com.elog.dto.LoginRequest;
import com.elog.dto.TokenRefreshRequest;
import com.elog.dto.TokenResponse;
import com.elog.dto.TokenRefreshResponse;
import com.elog.entity.RefreshToken;
import com.elog.entity.User;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.RefreshTokenRepository;
import com.elog.repository.UserRepository;
import com.elog.security.JwtUtils;
import com.elog.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;

    @Value("${elog.jwt.refresh-expiration-ms:604800000}") // Default 7 ngày
    private long refreshExpirationMs;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 1. Kiểm tra tài khoản tồn tại và active
        User user = userRepository.findByUsernameAndIsActiveTrue(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password", HttpStatus.UNAUTHORIZED));

        // 2. Thực hiện authenticate qua Spring Security AuthenticationManager
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password", HttpStatus.UNAUTHORIZED);
        }

        // 3. Xóa các Refresh Token cũ của user này trước khi tạo mới (tránh rác DB)
        refreshTokenRepository.deleteByUser(user);

        // 4. Tạo Access Token
        String accessToken = jwtUtils.generateAccessToken(user);

        // 5. Tạo Refresh Token mới
        RefreshToken refreshToken = createRefreshToken(user);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900L) // Hoặc tính toán động nếu cần
                .userId(user.getId())
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // Tìm Refresh Token trong DB
        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token is not in database!", HttpStatus.UNAUTHORIZED));

        // Kiểm tra xem Refresh Token đã hết hạn chưa
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Refresh token was expired. Please make a new signin request",
                    HttpStatus.UNAUTHORIZED);
        }

        User user = refreshToken.getUser();
        if (!user.getIsActive()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "User account is locked", HttpStatus.FORBIDDEN);
        }

        // Tạo Access Token mới
        String newAccessToken = jwtUtils.generateAccessToken(user);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .expiresIn(900L)
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        // Chỉ xóa token nếu tồn tại
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    // Tạo bản ghi Refresh Token lưu vào database
    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }
}