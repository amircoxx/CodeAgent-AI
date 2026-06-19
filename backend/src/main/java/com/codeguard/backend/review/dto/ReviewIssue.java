package com.codeguard.backend.review.dto;

import com.codeguard.backend.review.model.IssueCategory;
import com.codeguard.backend.review.model.Severity;

public record ReviewIssue(
    String title,
    Severity severity,
    IssueCategory category,
    String explanation,
    String suggestion,
    Integer lineNumber
) {
}
