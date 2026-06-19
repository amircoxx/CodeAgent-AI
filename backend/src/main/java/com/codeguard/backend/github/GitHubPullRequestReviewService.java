package com.codeguard.backend.github;

import com.codeguard.backend.config.CodeGuardGitHubProperties;
import com.codeguard.backend.review.ReviewService;
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

    return reviewService.createAnalyzedReview(
        request.projectId(),
        title,
        "Multiple",
        buildReviewInput(pullRequest, metadata, reviewableFiles)
    );
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
}
