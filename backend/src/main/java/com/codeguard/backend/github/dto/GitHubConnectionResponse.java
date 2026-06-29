package com.codeguard.backend.github.dto;

public record GitHubConnectionResponse(
    boolean connected,
    Long installationId,
    String accountLogin,
    String accountType
) {

  public static GitHubConnectionResponse disconnected() {
    return new GitHubConnectionResponse(false, null, null, null);
  }

  public static GitHubConnectionResponse connected(
      Long installationId,
      String accountLogin,
      String accountType
  ) {
    return new GitHubConnectionResponse(true, installationId, accountLogin, accountType);
  }
}
