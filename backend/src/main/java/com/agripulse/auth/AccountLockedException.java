package com.agripulse.auth;

/** Thrown when login is attempted on a locked or disabled account. */
public class AccountLockedException extends RuntimeException {
  public AccountLockedException(String message) {
    super(message);
  }
}
