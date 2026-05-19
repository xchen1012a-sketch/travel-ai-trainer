package com.teachagent;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, Object>> handleApiException(ApiException exception) {
    return ResponseEntity.status(exception.status()).body(Map.of(
      "success", false,
      "error", Map.of(
        "code", exception.code(),
        "message", exception.getMessage()
      )
    ));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
    return ResponseEntity.badRequest().body(Map.of(
      "success", false,
      "error", Map.of(
        "code", "INVALID_REQUEST",
        "message", "Request body is invalid."
      )
    ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleException(Exception exception) {
    return ResponseEntity.internalServerError().body(Map.of(
      "success", false,
      "error", Map.of(
        "code", "INTERNAL_ERROR",
        "message", exception.getMessage() == null ? "Internal server error." : exception.getMessage()
      )
    ));
  }
}
