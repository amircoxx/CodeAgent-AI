package com.codeguard.backend.github;

import java.util.List;

public interface GitHubClient {

  GitHubPullRequestMetadata fetchPullRequest(GitHubPullRequestRef pullRequest);

  List<GitHubPullRequestFile> fetchPullRequestFiles(GitHubPullRequestRef pullRequest);

  String createPullRequestComment(GitHubPullRequestRef pullRequest, String body);
}
