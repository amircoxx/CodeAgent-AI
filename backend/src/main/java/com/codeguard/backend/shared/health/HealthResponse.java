package com.codeguard.backend.shared.health;

import java.time.Instant;

public record HealthResponse(
    String status,
    String service,
    Instant timestamp
) {
}
