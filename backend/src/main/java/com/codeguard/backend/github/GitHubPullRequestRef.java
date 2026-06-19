package com.codeguard.backend.github;

public record GitHubPullRequestRef(
    String owner,
    String repo,
    int number
) {
}
