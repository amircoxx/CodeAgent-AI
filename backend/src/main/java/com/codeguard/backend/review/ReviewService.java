package com.codeguard.backend.review;

import com.codeguard.backend.review.dto.ReviewIssue;
import com.codeguard.backend.review.dto.ReviewRequest;
import com.codeguard.backend.review.dto.ReviewResponse;
import com.codeguard.backend.review.model.IssueCategory;
import com.codeguard.backend.review.model.IssueSeverity;
import com.codeguard.backend.review.model.RiskLevel;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

  public ReviewResponse reviewCode(ReviewRequest request) {
    List<ReviewIssue> issues = List.of(
        new ReviewIssue(
            IssueCategory.BUG,
            IssueSeverity.MEDIUM,
            "Guard against hidden runtime failures",
            "This mock review flags a possible logic path that could fail when real input differs from the sample.",
            "Add explicit validation and cover the edge case with a focused unit test."
        ),
        new ReviewIssue(
            IssueCategory.SECURITY,
            IssueSeverity.LOW,
            "Avoid leaking diagnostic output",
            "Console output is useful locally, but production paths should avoid exposing sensitive values in logs.",
            "Use structured logging and redact user-provided values before writing diagnostics."
        ),
        new ReviewIssue(
            IssueCategory.STYLE,
            IssueSeverity.LOW,
            "Make intent easier to scan",
            "The code can be easier to maintain when names describe the behavior being performed.",
            "Prefer descriptive names and keep each function focused on one responsibility."
        )
    );

    return new ReviewResponse(
        "Mock review completed. CodeGuard AI found a medium-risk issue profile with a few practical improvements.",
        RiskLevel.MEDIUM,
        request.language(),
        issues,
        buildImprovedCode(request)
    );
  }

  private String buildImprovedCode(ReviewRequest request) {
    return """
        // Mock improved %s code
        // Future versions will generate this with structured OpenAI responses.
        %s
        """.formatted(request.language(), request.code().trim());
  }
}
