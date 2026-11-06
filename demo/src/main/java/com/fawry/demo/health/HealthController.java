package com.fawry.demo.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * A quick check that the API answers, and which build is answering. The detailed
 * checks stay on /actuator/health.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String version;
    private final Instant startedAt = Instant.now();

    public HealthController(@Value("${app.version}") String version) {
        this.version = version;
    }

    @GetMapping
    public HealthStatus health() {
        return new HealthStatus("UP", version, startedAt);
    }
}
