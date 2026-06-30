package com.codeguard.backend.github.dto;

public record GitHubConnectionResponse(
    boolean connected,
    String accountLogin,
    String accountType
) {

  public static GitHubConnectionResponse disconnected() {
    return new GitHubConnectionResponse(false, null, null);
  }

  public static GitHubConnectionResponse connected(
      String accountLogin,
      String accountType
  ) {
    return new GitHubConnectionResponse(true, accountLogin, accountType);
  }
}
