package com.codeguard.backend.github;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidGitHubUrlException extends RuntimeException {

  public InvalidGitHubUrlException(String message) {
    super(message);
  }
}
