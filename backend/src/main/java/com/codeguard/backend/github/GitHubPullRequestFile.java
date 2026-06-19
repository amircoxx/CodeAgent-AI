package com.codeguard.backend.github;

public record GitHubPullRequestFile(
    String filename,
    String status,
    String patch
) {
}
