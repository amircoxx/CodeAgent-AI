package com.codeguard.backend.review.dto;

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
    Instant createdAt,
    List<ReviewIssue> issues,
    List<String> recommendedTests
) {
}
