package com.codeguard.backend.github;

public record GitHubPullRequestReviewRequest(
    Long projectId,
    String pullRequestUrl,
    String owner,
    String repo,
    Integer pullRequestNumber,
    Boolean postComment
) {

  public boolean isSelectedPullRequest() {
    return hasText(owner) && hasText(repo) && pullRequestNumber != null && pullRequestNumber > 0;
  }

  public boolean shouldPostComment() {
    return Boolean.TRUE.equals(postComment);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
