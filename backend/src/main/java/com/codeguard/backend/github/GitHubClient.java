package com.codeguard.backend.github;

import java.util.List;

public interface GitHubClient {

  GitHubInstallationMetadata fetchInstallation(Long installationId);

  GitHubPullRequestMetadata fetchPullRequest(GitHubPullRequestRef pullRequest);

  List<GitHubPullRequestFile> fetchPullRequestFiles(GitHubPullRequestRef pullRequest);

  String createPullRequestComment(GitHubPullRequestRef pullRequest, String body);
}
