package com.codeguard.backend.shared.error;

import java.time.Instant;

public record ApiErrorResponse(
    String message,
    int status,
    String path,
    Instant timestamp
) {
  public static ApiErrorResponse of(String message, int status, String path) {
    return new ApiErrorResponse(message, status, path, Instant.now());
  }
}
