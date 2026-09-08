package com.agripulse.common;

import com.agripulse.auth.AccountLockedException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Central error mapping. Client-safe messages; full details stay in server logs. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fields = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "Validation failed", fields));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
    return ResponseEntity.badRequest()
        .body(new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
  }

  @ExceptionHandler({AccessDeniedException.class, SecurityException.class})
  public ResponseEntity<ApiError> handleForbidden(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ApiError(HttpStatus.FORBIDDEN.value(), "Access denied"));
  }

  @ExceptionHandler(AccountLockedException.class)
  public ResponseEntity<ApiError> handleLocked(AccountLockedException ex) {
    return ResponseEntity.status(HttpStatus.LOCKED)
        .body(new ApiError(HttpStatus.LOCKED.value(), ex.getMessage()));
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ApiError> handleRateLimited(RateLimitExceededException ex) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(
            new ApiError(
                HttpStatus.TOO_MANY_REQUESTS.value(), "Too many requests, try again later"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error"));
  }
}
