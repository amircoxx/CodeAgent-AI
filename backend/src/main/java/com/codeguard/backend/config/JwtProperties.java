package com.codeguard.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codeguard.jwt")
public record JwtProperties(
    String secret,
    long expirationMs
) {
}
