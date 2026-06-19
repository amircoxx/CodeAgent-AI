package com.codeguard.backend.review.analysis;

import com.codeguard.backend.review.dto.ReviewIssue;
import com.codeguard.backend.review.dto.ReviewRequest;
import com.codeguard.backend.review.model.IssueCategory;
import com.codeguard.backend.review.model.Severity;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MockCodeAnalysisService implements CodeAnalysisService {

  @Override
  public CodeAnalysisResult analyzeCode(ReviewRequest request) {
    List<ReviewIssue> issues = List.of(
        new ReviewIssue(
            "Missing input validation",
            Severity.HIGH,
            IssueCategory.SECURITY,
            "The submitted code accepts input without showing clear validation before the value is used.",
            "Add request validation with DTO constraints or guard clauses before passing data into business logic.",
            12
        ),
        new ReviewIssue(
            "Missing negative-path tests",
            Severity.MEDIUM,
            IssueCategory.TESTING,
            "The code path should be covered with tests for invalid, empty, and unexpected input.",
            "Add focused unit tests that prove validation and failure handling work as expected.",
            null
        ),
        new ReviewIssue(
            "Improve separation of responsibilities",
            Severity.LOW,
            IssueCategory.MAINTAINABILITY,
            "Keeping validation, business logic, and persistence concerns separate will make this easier to change.",
            "Move reusable logic into a service method and keep controller or entry-point code thin.",
            null
        )
    );

    List<String> recommendedTests = List.of(
        "Test " + request.title().trim() + " with invalid input",
        "Test " + request.title().trim() + " with empty required fields",
        "Test " + request.title().trim() + " handles the expected success path"
    );

    return new CodeAnalysisResult(
        "This " + request.language().trim()
            + " code has a high-risk issue profile because it needs clearer input validation and stronger test coverage.",
        78,
        issues,
        recommendedTests
    );
  }
}
