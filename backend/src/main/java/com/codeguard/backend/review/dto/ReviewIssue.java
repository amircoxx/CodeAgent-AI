package com.codeguard.backend.review.dto;

import com.codeguard.backend.review.model.IssueCategory;
import com.codeguard.backend.review.model.IssueSeverity;

public record ReviewIssue(
    IssueCategory category,
    IssueSeverity severity,
    String title,
    String description,
    String suggestion
) {
}
