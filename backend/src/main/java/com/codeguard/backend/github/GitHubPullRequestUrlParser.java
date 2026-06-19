package com.codeguard.backend.github;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.stereotype.Component;

@Component
public class GitHubPullRequestUrlParser {

  public GitHubPullRequestRef parse(String url) {
    if (url == null || url.isBlank()) {
      throw new InvalidGitHubUrlException("Pull request URL is required");
    }

    URI uri;
    try {
      uri = new URI(url.trim());
    } catch (URISyntaxException exception) {
      throw invalid();
    }

    if (!"https".equalsIgnoreCase(uri.getScheme())
        || uri.getHost() == null
        || !"github.com".equalsIgnoreCase(uri.getHost())) {
      throw invalid();
    }

    String[] segments = uri.getPath().split("/");
    if (segments.length != 5 || !"pull".equals(segments[3])) {
      throw invalid();
    }

    String owner = segments[1];
    String repo = segments[2];
    String number = segments[4];
    if (owner.isBlank() || repo.isBlank() || !number.matches("\\d+")) {
      throw invalid();
    }

    return new GitHubPullRequestRef(owner, repo, Integer.parseInt(number));
  }

  private InvalidGitHubUrlException invalid() {
    return new InvalidGitHubUrlException(
        "GitHub pull request URL must match https://github.com/{owner}/{repo}/pull/{number}"
    );
  }
}
