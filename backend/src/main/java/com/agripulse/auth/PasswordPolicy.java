package com.agripulse.auth;

/**
 * Server-side password rules (never trust client validation alone).
 * Deliberately modest for low-literacy users: length plus letter+digit.
 */
public final class PasswordPolicy {

  public static final int MIN_LENGTH = 8;
  public static final int MAX_LENGTH = 100;

  private PasswordPolicy() {}

  public static void validate(String password) {
    if (password == null
        || password.length() < MIN_LENGTH
        || password.length() > MAX_LENGTH
        || !password.chars().anyMatch(Character::isLetter)
        || !password.chars().anyMatch(Character::isDigit)) {
      throw new IllegalArgumentException(
          "Password must be 8-100 characters and contain at least one letter and one digit");
    }
  }
}
