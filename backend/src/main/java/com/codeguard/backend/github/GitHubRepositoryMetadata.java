package com.codeguard.backend.github;

public record GitHubRepositoryMetadata(
    Long id,
    String owner,
    String name,
    String fullName,
    boolean privateRepository
) {
}
