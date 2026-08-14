package com.elog.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seeds a real expired refresh token for the L3 auth contract.
 *
 * The auth service correctly returns TOKEN_INVALID for arbitrary strings; the
 * TOKEN_EXPIRED branch requires a persisted refresh token whose expiry_date is
 * in the past.
 */
@SpringBootTest
@ActiveProfiles("dev")
class L3QaExpiredRefreshTokenFixtureTest {

    static final String TOKEN = "L3QA_EXPIRED_REFRESH_TOKEN";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedExpiredRefreshTokenForSystemUser() {
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE token = ?", TOKEN);
        jdbcTemplate.update("""
                INSERT INTO refresh_tokens (token, user_id, expiry_date, created_at)
                VALUES (?, 1, TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP))
                """, TOKEN);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE token = ? AND expiry_date < CURRENT_TIMESTAMP",
                Integer.class,
                TOKEN);
        assertThat(count).isEqualTo(1);
    }
}
