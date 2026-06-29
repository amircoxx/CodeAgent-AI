package com.codeguard.backend.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codeguard.github")
public record CodeGuardGitHubProperties(
    String token,
    String apiBaseUrl,
    Long appId,
    String appSlug,
    String privateKey,
    String setupCallbackUrl,
    String frontendConnectedRedirectUrl,
    int pendingStateTtlMinutes,
    int timeoutSeconds,
    int maxFiles,
    int maxPatchChars,
    boolean commentsEnabled
) {

  public boolean hasToken() {
    return token != null && !token.isBlank();
  }
}
