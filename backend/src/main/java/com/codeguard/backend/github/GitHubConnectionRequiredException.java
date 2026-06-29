package com.codeguard.backend.github;

public class GitHubConnectionRequiredException extends RuntimeException {

  public GitHubConnectionRequiredException(String message) {
    super(message);
  }
}
