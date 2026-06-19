package com.codeguard.backend.review;

import com.codeguard.backend.auth.CurrentUserService;
import com.codeguard.backend.project.ProjectNotFoundException;
import com.codeguard.backend.project.entity.ProjectEntity;
import com.codeguard.backend.project.repository.ProjectRepository;
import com.codeguard.backend.review.analysis.CodeAnalysisResult;
import com.codeguard.backend.review.analysis.CodeAnalysisService;
import com.codeguard.backend.review.entity.CodeReviewEntity;
import com.codeguard.backend.review.entity.ReviewIssueEntity;
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

  private ReviewResponse toResponse(CodeReviewEntity review) {
    return new ReviewResponse(
        review.getId(),
        review.getProject() == null ? null : review.getProject().getId(),
        review.getProject() == null ? null : review.getProject().getName(),
        review.getTitle(),
        review.getLanguage(),
        review.getSummary(),
        review.getRiskScore(),
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
}
