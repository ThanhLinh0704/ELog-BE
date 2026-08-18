package com.elog.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HashGeneratorTest {

    @Test
    void generateHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        System.out.println("ADMIN_HASH=" + encoder.encode("Admin@2025"));
        System.out.println("DEV_HASH=" + encoder.encode("Dev@2025"));
    }
}
