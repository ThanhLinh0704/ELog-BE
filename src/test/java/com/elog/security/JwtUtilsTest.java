package com.elog.security;

import com.elog.entity.Permission;
import com.elog.entity.Role;
import com.elog.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "ThisIsASecretKeyForELogApplicationTesting2026SuperSecureKey!");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000L); // 1 hour
    }

    @Test
    void generateAndValidateToken_success() {
        Role role = Role.builder().id(1L).name("DISPATCHER").permissions(Set.of(Permission.builder().id(1L).name("TRIP_VIEW").build())).build();
        User user = User.builder().id(10L).username("dispatcher01").roles(Set.of(role)).build();

        String token = jwtUtils.generateAccessToken(user);
        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));
        assertEquals("dispatcher01", jwtUtils.getUsernameFromToken(token));
    }

    @Test
    void validateToken_blacklisted_returnsFalse() {
        Role role = Role.builder().id(1L).name("DRIVER").build();
        User user = User.builder().id(20L).username("driver01").roles(Set.of(role)).build();

        String token = jwtUtils.generateAccessToken(user);
        jwtUtils.blacklistToken(token);

        assertFalse(jwtUtils.validateToken(token));
    }

    @Test
    void validateToken_malformed_returnsFalse() {
        assertFalse(jwtUtils.validateToken("invalid.jwt.token"));
        assertFalse(jwtUtils.validateToken(""));
    }
}
