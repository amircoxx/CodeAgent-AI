package com.codeguard.backend.github;

import com.codeguard.backend.config.CodeGuardGitHubProperties;
import com.codeguard.backend.review.ReviewService;
import com.codeguard.backend.review.dto.ReviewIssue;
import com.codeguard.backend.review.dto.ReviewResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GitHubPullRequestReviewService {

  private final GitHubPullRequestUrlParser urlParser;
  private final GitHubClient gitHubClient;
  private final ReviewService reviewService;
  private final CodeGuardGitHubProperties properties;

  public GitHubPullRequestReviewService(
      GitHubPullRequestUrlParser urlParser,
      GitHubClient gitHubClient,
      ReviewService reviewService,
      CodeGuardGitHubProperties properties
  ) {
    this.urlParser = urlParser;
    this.gitHubClient = gitHubClient;
    this.reviewService = reviewService;
    this.properties = properties;
  }

  public ReviewResponse reviewPullRequest(GitHubPullRequestReviewRequest request) {
    GitHubPullRequestRef pullRequest = urlParser.parse(request.pullRequestUrl());
    GitHubPullRequestMetadata metadata = gitHubClient.fetchPullRequest(pullRequest);
    List<GitHubPullRequestFile> reviewableFiles = gitHubClient.fetchPullRequestFiles(pullRequest)
        .stream()
        .filter(this::isReviewable)
        .limit(properties.maxFiles())
        .toList();

    if (reviewableFiles.isEmpty()) {
      throw new GitHubFetchException("GitHub pull request did not include reviewable text patches");
    }

    String title = "GitHub PR Review: "
        + pullRequest.owner() + "/" + pullRequest.repo() + "#" + pullRequest.number();

    ReviewResponse review = reviewService.createGitHubPullRequestReview(
        request.projectId(),
        title,
        "Multiple",
        buildReviewInput(pullRequest, metadata, reviewableFiles),
        pullRequest.owner(),
        pullRequest.repo(),
        pullRequest.number(),
        request.pullRequestUrl().trim(),
        blankToNull(metadata.title())
    );

    if (!request.shouldPostComment()) {
      return review;
    }

    return postReviewComment(pullRequest, review);
  }

  private ReviewResponse postReviewComment(GitHubPullRequestRef pullRequest, ReviewResponse review) {
    if (!properties.commentsEnabled()) {
      return reviewService.markGitHubCommentFailed(
          review.id(),
          "GitHub PR comments are disabled for this environment."
      );
    }

    if (!properties.hasToken()) {
      return reviewService.markGitHubCommentFailed(
          review.id(),
          "GitHub PR comments require CODEGUARD_GITHUB_TOKEN."
      );
    }

    try {
      String commentUrl = gitHubClient.createPullRequestComment(
          pullRequest,
          buildCommentBody(review)
      );
      return reviewService.markGitHubCommentPosted(review.id(), commentUrl);
    } catch (GitHubFetchException exception) {
      return reviewService.markGitHubCommentFailed(
          review.id(),
          "Could not post the GitHub PR comment. The review was saved."
      );
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private boolean isReviewable(GitHubPullRequestFile file) {
    if (file.filename() == null || file.filename().isBlank()) {
      return false;
    }
    if (file.patch() == null || file.patch().isBlank()) {
      return false;
    }
    if (file.patch().length() > properties.maxPatchChars()) {
      return false;
    }

    String filename = file.filename();
    return !filename.endsWith("package-lock.json")
        && !filename.endsWith("yarn.lock")
        && !filename.endsWith("pnpm-lock.yaml")
        && !filename.startsWith("target/")
        && !filename.startsWith("build/")
        && !filename.startsWith("dist/")
        && !filename.startsWith(".next/");
  }

  private String buildReviewInput(
      GitHubPullRequestRef pullRequest,
      GitHubPullRequestMetadata metadata,
      List<GitHubPullRequestFile> files
  ) {
    StringBuilder builder = new StringBuilder();
    builder.append("Repository: ")
        .append(pullRequest.owner()).append("/").append(pullRequest.repo()).append("\n");
    builder.append("Pull Request: #")
        .append(pullRequest.number()).append(" - ").append(metadata.title()).append("\n");
    if (metadata.author() != null && !metadata.author().isBlank()) {
      builder.append("Author: ").append(metadata.author()).append("\n");
    }

    builder.append("Changed files:\n");
    files.forEach(file -> builder.append("- ").append(file.filename()).append("\n"));

    builder.append("\nDiff:\n");
    files.forEach(file -> builder
        .append("File: ").append(file.filename()).append("\n")
        .append("Status: ").append(file.status()).append("\n")
        .append("Patch:\n")
        .append(file.patch()).append("\n\n"));

    return builder.toString().trim();
  }

  private String buildCommentBody(ReviewResponse review) {
    StringBuilder builder = new StringBuilder();
    builder.append("<!-- codeguard-ai-review -->\n\n")
        .append("## CodeGuard AI Review\n\n")
        .append("**Risk Score:** ")
        .append(review.riskScore())
        .append("/100 - ")
        .append(riskLabel(review.riskScore()))
        .append("\n")
        .append("**Source:** GitHub PR Review\n\n")
        .append("### Summary\n\n")
        .append(review.summary())
        .append("\n\n");

    if (!review.issues().isEmpty()) {
      builder.append("### Top Issues\n\n");
      List<ReviewIssue> topIssues = review.issues().stream().limit(5).toList();
      for (int index = 0; index < topIssues.size(); index++) {
        ReviewIssue issue = topIssues.get(index);
        builder.append(index + 1)
            .append(". **[")
            .append(issue.severity())
            .append("] ")
            .append(issue.title())
            .append("**\n")
            .append("   Category: ")
            .append(issue.category())
            .append("\n")
            .append("   Suggestion: ")
            .append(issue.suggestion())
            .append("\n\n");
      }
    }

    if (!review.recommendedTests().isEmpty()) {
      builder.append("### Recommended Tests\n\n");
      review.recommendedTests().stream()
          .limit(5)
          .forEach(test -> builder.append("- ").append(test).append("\n"));
      builder.append("\n");
    }

    builder.append("---\n\nGenerated by CodeGuard AI.");
    return builder.toString();
  }

  private String riskLabel(int riskScore) {
    if (riskScore >= 90) {
      return "Critical Risk";
    }
    if (riskScore >= 70) {
      return "High Risk";
    }
    if (riskScore >= 40) {
      return "Medium Risk";
    }
    return "Low Risk";
  }
}
