package com.elog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies Spring context loads successfully.
 * Requires: elog_db running locally with Flyway migrations applied.
 * Run: mvn test
 */
@SpringBootTest
@ActiveProfiles("dev")
class ElogApplicationTests {

    @Test
    void contextLoads() {
        // If this passes, Spring context + Flyway + JPA are all wired correctly.
    }
}
