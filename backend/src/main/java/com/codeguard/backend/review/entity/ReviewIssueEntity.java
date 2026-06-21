package com.codeguard.backend.review.entity;

import com.codeguard.backend.review.model.IssueCategory;
import com.codeguard.backend.review.model.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_issues")
public class ReviewIssueEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String severity;

  @Column(nullable = false)
  private String category;

  @Column(nullable = false, columnDefinition = "text")
  private String explanation;

  @Column(nullable = false, columnDefinition = "text")
  private String suggestion;

  private Integer lineNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "code_review_id", nullable = false)
  private CodeReviewEntity codeReview;

  protected ReviewIssueEntity() {
  }

  public ReviewIssueEntity(
      String title,
      Severity severity,
      IssueCategory category,
      String explanation,
      String suggestion,
      Integer lineNumber
  ) {
    this.title = title;
    this.severity = severity.name();
    this.category = category.name();
    this.explanation = explanation;
    this.suggestion = suggestion;
    this.lineNumber = lineNumber;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public Severity getSeverity() {
    if (severity == null || severity.isBlank()) {
      return Severity.MEDIUM;
    }

    try {
      return Severity.valueOf(severity);
    } catch (IllegalArgumentException exception) {
      return Severity.MEDIUM;
    }
  }

  public IssueCategory getCategory() {
    if (category == null || category.isBlank()) {
      return IssueCategory.MAINTAINABILITY;
    }

    return switch (category) {
      case "BUG" -> IssueCategory.BUG_RISK;
      case "STYLE" -> IssueCategory.READABILITY;
      default -> {
        try {
          yield IssueCategory.valueOf(category);
        } catch (IllegalArgumentException exception) {
          yield IssueCategory.MAINTAINABILITY;
        }
      }
    };
  }

  public String getExplanation() {
    return explanation;
  }

  public String getSuggestion() {
    return suggestion;
  }

  public Integer getLineNumber() {
    return lineNumber;
  }

  public CodeReviewEntity getCodeReview() {
    return codeReview;
  }

  void setCodeReview(CodeReviewEntity codeReview) {
    this.codeReview = codeReview;
  }
}
