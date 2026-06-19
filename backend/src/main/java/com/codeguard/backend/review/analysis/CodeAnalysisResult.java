package com.codeguard.backend.review.analysis;

import com.codeguard.backend.review.dto.ReviewIssue;
import java.util.List;

public record CodeAnalysisResult(
    String summary,
    int riskScore,
    List<ReviewIssue> issues,
    List<String> recommendedTests
) {
}
