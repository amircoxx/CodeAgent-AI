package com.codeguard.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codeguard.ai")
public record CodeGuardAiProperties(
    boolean enabled,
    String apiKey,
    String model,
    String endpoint,
    int timeoutSeconds
) {

  public boolean hasApiKey() {
    return apiKey != null && !apiKey.isBlank();
  }
}
