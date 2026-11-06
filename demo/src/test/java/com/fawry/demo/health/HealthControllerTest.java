package com.fawry.demo.health;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Web layer tests for GET /api/health, without starting a server. */
class HealthControllerTest {

    private final MockMvcTester mvc = MockMvcTester.of(new HealthController("sha-1234567"));

    @Test
    void reportsUpAndTheRunningVersion() {
        assertThat(mvc.get().uri("/api/health"))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("{\"status\": \"UP\", \"version\": \"sha-1234567\"}");
    }

    @Test
    void reportsWhenThisInstanceStartedAsAReadableTimestamp() {
        assertThat(mvc.get().uri("/api/health"))
                .bodyJson().extractingPath("$.startedAt").asString()
                .satisfies(text -> assertThat(Instant.parse(text)).isBeforeOrEqualTo(Instant.now()));
    }
}
