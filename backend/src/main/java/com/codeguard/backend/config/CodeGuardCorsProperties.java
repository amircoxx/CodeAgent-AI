package com.codeguard.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codeguard.cors")
public record CodeGuardCorsProperties(
    List<String> allowedOrigins
) {

  public List<String> allowedOriginsOrDefault() {
    if (allowedOrigins == null || allowedOrigins.isEmpty()) {
      return List.of(
          "http://localhost:3000",
          "http://localhost:3001",
          "http://localhost:3002"
      );
    }
    return allowedOrigins;
  }
}
