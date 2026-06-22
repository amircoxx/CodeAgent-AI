package com.codeguard.backend.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codeguard.account")
public record CodeGuardAccountProperties(
    long passwordCodeTtlMinutes,
    boolean exposeDevVerificationCode
) {

  public Duration passwordCodeTtl() {
    return Duration.ofMinutes(passwordCodeTtlMinutes <= 0 ? 15 : passwordCodeTtlMinutes);
  }
}
