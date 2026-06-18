package com.codeguard.backend.review.dto;

import com.codeguard.backend.review.model.RiskLevel;
import java.util.List;

public record ReviewResponse(
    String summary,
    RiskLevel riskLevel,
    String language,
    List<ReviewIssue> issues,
    String improvedCode
) {
}
