package com.codeguard.backend.review.dto;

import com.codeguard.backend.review.model.ReviewSource;
import java.time.Instant;
import java.util.List;

public record ReviewResponse(
    Long id,
    Long projectId,
    String projectName,
    String title,
    String language,
    String summary,
    int riskScore,
    ReviewSource source,
    String githubOwner,
    String githubRepo,
    Integer githubPullRequestNumber,
    String githubPullRequestUrl,
    String githubPullRequestTitle,
    boolean githubCommentPosted,
    String githubCommentUrl,
    String githubCommentError,
    Instant createdAt,
    List<ReviewIssue> issues,
    List<String> recommendedTests
) {
}
