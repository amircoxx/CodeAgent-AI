package com.codeguard.backend.review;

import com.codeguard.backend.auth.CurrentUserService;
import com.codeguard.backend.project.ProjectNotFoundException;
import com.codeguard.backend.project.entity.ProjectEntity;
import com.codeguard.backend.project.repository.ProjectRepository;
import com.codeguard.backend.review.analysis.CodeAnalysisResult;
import com.codeguard.backend.review.analysis.CodeAnalysisService;
import com.codeguard.backend.review.entity.CodeReviewEntity;
import com.codeguard.backend.review.entity.ReviewIssueEntity;
import com.codeguard.backend.review.model.ReviewSource;
import com.codeguard.backend.review.dto.ReviewIssue;
import com.codeguard.backend.review.dto.ReviewRequest;
import com.codeguard.backend.review.dto.ReviewResponse;
import com.codeguard.backend.review.repository.CodeReviewRepository;
import com.codeguard.backend.user.entity.UserEntity;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

  private final CodeReviewRepository codeReviewRepository;
  private final ProjectRepository projectRepository;
  private final CodeAnalysisService codeAnalysisService;
  private final CurrentUserService currentUserService;

  public ReviewService(
      CodeReviewRepository codeReviewRepository,
      ProjectRepository projectRepository,
      CodeAnalysisService codeAnalysisService,
      CurrentUserService currentUserService
  ) {
    this.codeReviewRepository = codeReviewRepository;
    this.projectRepository = projectRepository;
    this.codeAnalysisService = codeAnalysisService;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public ReviewResponse createReview(ReviewRequest request) {
    return createAnalyzedReview(
        request.projectId(),
        request.title(),
        request.language(),
        request.code()
    );
  }

  @Transactional
  public ReviewResponse createAnalyzedReview(
      Long projectId,
      String title,
      String language,
      String submittedCode
  ) {
    return createAnalyzedReview(
        projectId,
        title,
        language,
        submittedCode,
        ReviewMetadata.manual()
    );
  }

  @Transactional
  public ReviewResponse createGitHubPullRequestReview(
      Long projectId,
      String title,
      String language,
      String submittedCode,
      String githubOwner,
      String githubRepo,
      Integer githubPullRequestNumber,
      String githubPullRequestUrl,
      String githubPullRequestTitle
  ) {
    return createAnalyzedReview(
        projectId,
        title,
        language,
        submittedCode,
        ReviewMetadata.githubPullRequest(
            githubOwner,
            githubRepo,
            githubPullRequestNumber,
            githubPullRequestUrl,
            githubPullRequestTitle
        )
    );
  }

  private ReviewResponse createAnalyzedReview(
      Long projectId,
      String title,
      String language,
      String submittedCode,
      ReviewMetadata metadata
  ) {
    UserEntity user = currentUserService.getCurrentUser();
    ReviewRequest request = new ReviewRequest(projectId, title, language, submittedCode);
    CodeAnalysisResult analysis = codeAnalysisService.analyzeCode(request);
    CodeReviewEntity review = new CodeReviewEntity(
        request.title().trim(),
        request.language().trim(),
        request.code().trim(),
        analysis.summary(),
        analysis.riskScore()
    );
    review.setUser(user);

    if (metadata.source() == ReviewSource.GITHUB_PR) {
      review.markAsGitHubPullRequestReview(
          metadata.githubOwner(),
          metadata.githubRepo(),
          metadata.githubPullRequestNumber(),
          metadata.githubPullRequestUrl(),
          metadata.githubPullRequestTitle()
      );
    }

    if (projectId != null) {
      ProjectEntity project = projectRepository.findByIdAndUserId(projectId, user.getId())
          .orElseThrow(() -> new ProjectNotFoundException(projectId));
      review.setProject(project);
    }

    analysis.issues().forEach(issue -> review.addIssue(new ReviewIssueEntity(
        issue.title(),
        issue.severity(),
        issue.category(),
        issue.explanation(),
        issue.suggestion(),
        issue.lineNumber()
    )));
    analysis.recommendedTests().forEach(review::addRecommendedTest);

    CodeReviewEntity savedReview = codeReviewRepository.save(review);
    return toResponse(savedReview);
  }

  @Transactional(readOnly = true)
  public List<ReviewResponse> getReviews() {
    UserEntity user = currentUserService.getCurrentUser();
    return codeReviewRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public ReviewResponse getReview(Long id) {
    UserEntity user = currentUserService.getCurrentUser();
    CodeReviewEntity review = codeReviewRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ReviewNotFoundException(id));
    return toResponse(review);
  }

  @Transactional
  public ReviewResponse markGitHubCommentPosted(Long id, String commentUrl) {
    UserEntity user = currentUserService.getCurrentUser();
    CodeReviewEntity review = codeReviewRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ReviewNotFoundException(id));
    review.markGitHubCommentPosted(blankToNull(commentUrl));
    return toResponse(review);
  }

  @Transactional
  public ReviewResponse markGitHubCommentFailed(Long id, String message) {
    UserEntity user = currentUserService.getCurrentUser();
    CodeReviewEntity review = codeReviewRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ReviewNotFoundException(id));
    review.markGitHubCommentFailed(message);
    return toResponse(review);
  }

  private ReviewResponse toResponse(CodeReviewEntity review) {
    return new ReviewResponse(
        review.getId(),
        review.getProject() == null ? null : review.getProject().getId(),
        review.getProject() == null ? null : review.getProject().getName(),
        review.getTitle(),
        review.getLanguage(),
        review.getSummary(),
        review.getRiskScore(),
        review.getSource(),
        review.getGithubOwner(),
        review.getGithubRepo(),
        review.getGithubPullRequestNumber(),
        review.getGithubPullRequestUrl(),
        review.getGithubPullRequestTitle(),
        review.isGithubCommentPosted(),
        review.getGithubCommentUrl(),
        review.getGithubCommentError(),
        review.getCreatedAt(),
        review.getIssues().stream()
            .map(issue -> new ReviewIssue(
                issue.getTitle(),
                issue.getSeverity(),
                issue.getCategory(),
                issue.getExplanation(),
                issue.getSuggestion(),
                issue.getLineNumber()
            ))
            .toList(),
        List.copyOf(review.getRecommendedTests())
    );
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private record ReviewMetadata(
      ReviewSource source,
      String githubOwner,
      String githubRepo,
      Integer githubPullRequestNumber,
      String githubPullRequestUrl,
      String githubPullRequestTitle
  ) {

    private static ReviewMetadata manual() {
      return new ReviewMetadata(ReviewSource.MANUAL, null, null, null, null, null);
    }

    private static ReviewMetadata githubPullRequest(
        String githubOwner,
        String githubRepo,
        Integer githubPullRequestNumber,
        String githubPullRequestUrl,
        String githubPullRequestTitle
    ) {
      return new ReviewMetadata(
          ReviewSource.GITHUB_PR,
          githubOwner,
          githubRepo,
          githubPullRequestNumber,
          githubPullRequestUrl,
          githubPullRequestTitle
      );
    }
  }
}
