package com.codeguard.backend.github;

public record GitHubInstallationMetadata(
    Long installationId,
    String accountLogin,
    String accountType
) {
}
