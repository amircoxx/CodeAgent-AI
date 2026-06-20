package com.codeguard.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codeguard.github")
public record CodeGuardGitHubProperties(
    String token,
    String apiBaseUrl,
    int timeoutSeconds,
    int maxFiles,
    int maxPatchChars,
    boolean commentsEnabled
) {

  public boolean hasToken() {
    return token != null && !token.isBlank();
  }
}
