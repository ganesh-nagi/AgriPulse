package com.agripulse.common;

import java.time.LocalDateTime;
import java.util.Map;

/** Secure error contract: status + message only, never stack traces or internals. */
public class ApiError {

  private final LocalDateTime timestamp = LocalDateTime.now();
  private final int status;
  private final String message;
  private final Map<String, String> fieldErrors;

  public ApiError(int status, String message) {
    this(status, message, null);
  }

  public ApiError(int status, String message, Map<String, String> fieldErrors) {
    this.status = status;
    this.message = message;
    this.fieldErrors = fieldErrors;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public int getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public Map<String, String> getFieldErrors() {
    return fieldErrors;
  }
}
