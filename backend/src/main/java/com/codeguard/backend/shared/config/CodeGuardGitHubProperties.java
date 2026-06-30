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
    String oauthClientId,
    String oauthClientSecret,
    String oauthScope,
    String oauthAuthorizeUrl,
    String oauthTokenUrl,
    String oauthCallbackUrl,
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

  public boolean hasOAuthCredentials() {
    return oauthClientId != null && !oauthClientId.isBlank()
        && oauthClientSecret != null && !oauthClientSecret.isBlank();
  }

  public String resolvedOAuthScope() {
    return oauthScope == null || oauthScope.isBlank() ? "repo" : oauthScope.trim();
  }
}
