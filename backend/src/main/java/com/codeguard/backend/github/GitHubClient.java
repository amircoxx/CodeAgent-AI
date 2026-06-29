package com.codeguard.backend.github;

import java.util.List;

public interface GitHubClient {

  GitHubInstallationMetadata fetchInstallation(Long installationId);

  List<GitHubRepositoryMetadata> listInstallationRepositories(String installationToken);

  List<GitHubPullRequestSummary> listPullRequests(
      String installationToken,
      GitHubPullRequestRef pullRequest
  );

  GitHubPullRequestMetadata fetchPullRequest(GitHubPullRequestRef pullRequest);

  GitHubPullRequestMetadata fetchPullRequest(
      String installationToken,
      GitHubPullRequestRef pullRequest
  );

  List<GitHubPullRequestFile> fetchPullRequestFiles(GitHubPullRequestRef pullRequest);

  List<GitHubPullRequestFile> fetchPullRequestFiles(
      String installationToken,
      GitHubPullRequestRef pullRequest
  );

  String createPullRequestComment(GitHubPullRequestRef pullRequest, String body);
}
