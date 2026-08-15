package com.elog.integration;

import com.elog.dto.request.LoginRequest;
import com.elog.dto.request.TokenRefreshRequest;
import com.elog.dto.response.TokenResponse;
import com.elog.entity.RefreshToken;
import com.elog.entity.User;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.RefreshTokenRepository;
import com.elog.repository.UserRepository;
import com.elog.service.AuthService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class AuthServiceIntegrationTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;

    @Test
    void l2Atk01LoginPersistsOneRefreshTokenWithSevenDayExpiry() {
        User user = user(true);
        TokenResponse response = authService.login(login(user.getUsername(), "R5-secret"));
        entityManager.flush();
        List<RefreshToken> tokens = tokensFor(user);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst().getToken()).isEqualTo(response.getRefreshToken());
        assertThat(tokens.getFirst().getExpiryDate()).isAfter(Instant.now().plusSeconds(6 * 24 * 3600));
    }

    @Test
    void l2Atk02LoginReplacesAllExistingRefreshTokensWithOne() {
        User user = user(true);
        for (int index = 0; index < 3; index++) {
            refreshTokenRepository.save(RefreshToken.builder().user(user)
                    .token(UUID.randomUUID().toString()).expiryDate(Instant.now().plusSeconds(3600)).build());
        }
        entityManager.flush();

        TokenResponse response = authService.login(login(user.getUsername(), "R5-secret"));
        entityManager.flush();

        assertThat(tokensFor(user)).singleElement()
                .extracting(RefreshToken::getToken).isEqualTo(response.getRefreshToken());
    }

    @Test
    void l2Atk03InvalidPasswordCreatesNoRefreshToken() {
        User user = user(true);
        assertThatThrownBy(() -> authService.login(login(user.getUsername(), "wrong")))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        assertThat(tokensFor(user)).isEmpty();
    }

    @Test
    void l2Atk04DisabledAccountCreatesNoRefreshToken() {
        User user = user(false);
        assertThatThrownBy(() -> authService.login(login(user.getUsername(), "R5-secret")))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_DISABLED));
        assertThat(tokensFor(user)).isEmpty();
    }

    @Test
    void l2Atk05RefreshReturnsNewAccessTokenWithoutChangingRefreshRow() {
        User user = user(true);
        RefreshToken token = refreshTokenRepository.save(RefreshToken.builder().user(user)
                .token(UUID.randomUUID().toString()).expiryDate(Instant.now().plusSeconds(3600)).build());
        entityManager.flush();
        long before = refreshTokenRepository.count();
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(token.getToken());

        assertThat(authService.refresh(request).getAccessToken()).isNotBlank();
        entityManager.flush();
        assertThat(refreshTokenRepository.count()).isEqualTo(before);
        assertThat(refreshTokenRepository.findByToken(token.getToken())).isPresent();
    }

    private User user(boolean active) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.saveAndFlush(User.builder()
                .username("r5auth" + suffix)
                .email("r5auth" + suffix + "@example.test")
                .fullName("Report 5 Auth")
                .passwordHash(passwordEncoder.encode("R5-secret"))
                .isActive(active)
                .build());
    }

    private LoginRequest login(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private List<RefreshToken> tokensFor(User user) {
        return refreshTokenRepository.findAll().stream()
                .filter(token -> token.getUser().getId().equals(user.getId())).toList();
    }
}

