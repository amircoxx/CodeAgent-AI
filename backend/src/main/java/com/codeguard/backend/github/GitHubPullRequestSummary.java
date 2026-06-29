package com.codeguard.backend.github;

public record GitHubPullRequestSummary(
    int number,
    String title,
    String author,
    String url
) {
}
