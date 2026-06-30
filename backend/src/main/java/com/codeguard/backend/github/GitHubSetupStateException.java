package com.codeguard.backend.github;

public class GitHubSetupStateException extends RuntimeException {

  public GitHubSetupStateException() {
    super("GitHub connection state is invalid or expired.");
  }

  public GitHubSetupStateException(String message) {
    super(message);
  }
}
