package com.codeguard.backend.github.dto;

public record GitHubConnectUrlResponse(
    String connectUrl,
    String state
) {
}
