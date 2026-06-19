package com.codeguard.backend.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GitHubPullRequestUrlParserTest {

  private final GitHubPullRequestUrlParser parser = new GitHubPullRequestUrlParser();

  @Test
  void parseExtractsOwnerRepoAndPullRequestNumber() {
    GitHubPullRequestRef pullRequest = parser.parse("https://github.com/openai/codex/pull/123");

    assertThat(pullRequest.owner()).isEqualTo("openai");
    assertThat(pullRequest.repo()).isEqualTo("codex");
    assertThat(pullRequest.number()).isEqualTo(123);
  }

  @Test
  void parseRejectsInvalidUrl() {
    assertThatThrownBy(() -> parser.parse("https://github.com/openai/codex/issues/123"))
        .isInstanceOf(InvalidGitHubUrlException.class)
        .hasMessage("GitHub pull request URL must match https://github.com/{owner}/{repo}/pull/{number}");
  }

  @Test
  void parseRejectsNonNumericPullRequestNumber() {
    assertThatThrownBy(() -> parser.parse("https://github.com/openai/codex/pull/not-a-number"))
        .isInstanceOf(InvalidGitHubUrlException.class);
  }
}
