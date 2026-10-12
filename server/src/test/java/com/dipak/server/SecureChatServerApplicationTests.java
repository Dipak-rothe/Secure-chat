package com.dipak.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifies the Spring application context loads successfully.
 * Uses the 'test' profile (H2, no Redis).
 */
@SpringBootTest
@ActiveProfiles("test")
class SecureChatServerApplicationTests {

    @Test
    void contextLoads() {
        // If the context loads without throwing, this test passes.
    }
}
