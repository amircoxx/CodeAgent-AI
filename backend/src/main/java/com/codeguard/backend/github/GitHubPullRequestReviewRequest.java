package com.codeguard.backend.github;

import jakarta.validation.constraints.NotBlank;

public record GitHubPullRequestReviewRequest(
    Long projectId,
    @NotBlank(message = "Pull request URL is required") String pullRequestUrl
) {
}
