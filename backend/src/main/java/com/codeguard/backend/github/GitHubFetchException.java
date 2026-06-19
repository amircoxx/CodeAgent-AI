package com.codeguard.backend.github;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class GitHubFetchException extends RuntimeException {

  public GitHubFetchException(String message) {
    super(message);
  }

  public GitHubFetchException(String message, Throwable cause) {
    super(message, cause);
  }
}
