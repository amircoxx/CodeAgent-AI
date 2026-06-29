package com.codeguard.backend.github;

import com.codeguard.backend.github.dto.GitHubConnectionResponse;
import com.codeguard.backend.github.dto.GitHubConnectUrlResponse;
import com.codeguard.backend.review.dto.ReviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

  private final GitHubPullRequestReviewService pullRequestReviewService;
  private final GitHubConnectionService gitHubConnectionService;

  public GitHubController(
      GitHubPullRequestReviewService pullRequestReviewService,
      GitHubConnectionService gitHubConnectionService
  ) {
    this.pullRequestReviewService = pullRequestReviewService;
    this.gitHubConnectionService = gitHubConnectionService;
  }

  @GetMapping("/connection")
  public GitHubConnectionResponse getConnection() {
    return gitHubConnectionService.getConnection();
  }

  @GetMapping("/connect-url")
  public GitHubConnectUrlResponse getConnectUrl() {
    return gitHubConnectionService.createConnectUrl();
  }

  @GetMapping("/setup")
  public ResponseEntity<Void> completeSetup(
      @RequestParam("installation_id") Long installationId,
      @RequestParam String state
  ) {
    return ResponseEntity
        .status(302)
        .header("Location", gitHubConnectionService.completeSetup(installationId, state))
        .build();
  }

  @GetMapping("/repositories")
  public java.util.List<GitHubRepositoryMetadata> listRepositories() {
    return gitHubConnectionService.listRepositories();
  }

  @GetMapping("/repositories/{owner}/{repo}/pull-requests")
  public java.util.List<GitHubPullRequestSummary> listPullRequests(
      @org.springframework.web.bind.annotation.PathVariable String owner,
      @org.springframework.web.bind.annotation.PathVariable String repo
  ) {
    return gitHubConnectionService.listPullRequests(owner, repo);
  }

  @PostMapping("/pull-request-review")
  public ReviewResponse reviewPullRequest(
      @Valid @RequestBody GitHubPullRequestReviewRequest request
  ) {
    return pullRequestReviewService.reviewPullRequest(request);
  }
}
