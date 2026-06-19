package com.codeguard.backend.review.entity;

import com.codeguard.backend.project.entity.ProjectEntity;
import com.codeguard.backend.user.entity.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "code_reviews")
public class CodeReviewEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String language;

  @Column(nullable = false, columnDefinition = "text")
  private String submittedCode;

  @Column(nullable = false, columnDefinition = "text")
  private String summary;

  @Column(nullable = false)
  private int riskScore;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private ProjectEntity project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserEntity user;

  @OneToMany(mappedBy = "codeReview", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ReviewIssueEntity> issues = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "code_review_recommended_tests", joinColumns = @JoinColumn(name = "code_review_id"))
  @OrderColumn(name = "position")
  @Column(name = "description", nullable = false, columnDefinition = "text")
  private List<String> recommendedTests = new ArrayList<>();

  protected CodeReviewEntity() {
  }

  public CodeReviewEntity(
      String title,
      String language,
      String submittedCode,
      String summary,
      int riskScore
  ) {
    this.title = title;
    this.language = language;
    this.submittedCode = submittedCode;
    this.summary = summary;
    this.riskScore = riskScore;
    this.createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getLanguage() {
    return language;
  }

  public String getSubmittedCode() {
    return submittedCode;
  }

  public String getSummary() {
    return summary;
  }

  public int getRiskScore() {
    return riskScore;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public ProjectEntity getProject() {
    return project;
  }

  public UserEntity getUser() {
    return user;
  }

  public List<ReviewIssueEntity> getIssues() {
    return issues;
  }

  public List<String> getRecommendedTests() {
    return recommendedTests;
  }

  public void addIssue(ReviewIssueEntity issue) {
    issues.add(issue);
    issue.setCodeReview(this);
  }

  public void addRecommendedTest(String recommendedTest) {
    recommendedTests.add(recommendedTest);
  }

  public void setProject(ProjectEntity project) {
    this.project = project;
  }

  public void setUser(UserEntity user) {
    this.user = user;
  }
}
