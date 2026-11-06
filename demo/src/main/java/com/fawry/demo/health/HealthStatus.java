package com.fawry.demo.health;

import java.time.Instant;

/**
 * Body of GET /api/health.
 *
 * @param status    always UP: the app can only answer while it is running
 * @param version   the build that is running, e.g. sha-24405c0 (the app.version setting)
 * @param startedAt when this instance started, so each new deployment shows a new time
 */
public record HealthStatus(String status, String version, Instant startedAt) {
}
