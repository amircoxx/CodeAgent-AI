package com.codeguard.backend.review.entity;

import com.codeguard.backend.review.model.IssueCategory;
import com.codeguard.backend.review.model.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Severity severity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private IssueCategory category;

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
    this.severity = severity;
    this.category = category;
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
    return severity;
  }

  public IssueCategory getCategory() {
    return category;
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
