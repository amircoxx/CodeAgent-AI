package com.codeguard.backend.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException exception,
      HttpServletRequest request
  ) {
    Set<String> messages = new LinkedHashSet<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      messages.add(fieldError.getDefaultMessage());
    }

    String message = messages.isEmpty()
        ? "Request validation failed"
        : String.join("; ", messages);

    return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
      NoResourceFoundException exception,
      HttpServletRequest request
  ) {
    return build(HttpStatus.NOT_FOUND, "Resource not found: " + request.getRequestURI(), request.getRequestURI());
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiErrorResponse> handleRuntime(
      RuntimeException exception,
      HttpServletRequest request
  ) {
    ResponseStatus responseStatus = exception.getClass().getAnnotation(ResponseStatus.class);
    if (responseStatus != null) {
      HttpStatus status = responseStatus.value();
      return build(status, cleanMessage(exception, status), request.getRequestURI());
    }

    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected server error occurred",
        request.getRequestURI()
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGeneric(
      Exception exception,
      HttpServletRequest request
  ) {
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected server error occurred",
        request.getRequestURI()
    );
  }

  private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, String path) {
    return ResponseEntity
        .status(status)
        .body(ApiErrorResponse.of(message, status.value(), path));
  }

  private String cleanMessage(RuntimeException exception, HttpStatus status) {
    if (exception.getMessage() == null || exception.getMessage().isBlank()) {
      return status.getReasonPhrase();
    }
    return exception.getMessage();
  }
}
