package com.codeguard.backend.github;

import com.codeguard.backend.review.dto.ReviewResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

  private final GitHubPullRequestReviewService pullRequestReviewService;

  public GitHubController(GitHubPullRequestReviewService pullRequestReviewService) {
    this.pullRequestReviewService = pullRequestReviewService;
  }

  @PostMapping("/pull-request-review")
  public ReviewResponse reviewPullRequest(
      @Valid @RequestBody GitHubPullRequestReviewRequest request
  ) {
    return pullRequestReviewService.reviewPullRequest(request);
  }
}
