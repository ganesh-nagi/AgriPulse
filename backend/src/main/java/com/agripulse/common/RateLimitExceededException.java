package com.agripulse.common;

/** Thrown when a client exceeds the fixed-window request budget. Maps to 429. */
public class RateLimitExceededException extends RuntimeException {
  public RateLimitExceededException() {
    super("Rate limit exceeded");
  }
}
