package com.codeguard.backend.account;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPasswordVerificationException extends RuntimeException {

  public InvalidPasswordVerificationException() {
    super("The verification code is invalid, expired, or already used.");
  }
}
