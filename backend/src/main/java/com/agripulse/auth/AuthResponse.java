package com.agripulse.auth;

/**
 * Returned after register/login/refresh. Access token goes in the Authorization
 * header; the opaque refresh token is exchanged only at /api/auth/refresh.
 */
public class AuthResponse {

  private final String token;
  private final String refreshToken;
  private final String email;
  private final String role;

  public AuthResponse(String token, String refreshToken, String email, String role) {
    this.token = token;
    this.refreshToken = refreshToken;
    this.email = email;
    this.role = role;
  }

  public String getToken() {
    return token;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public String getEmail() {
    return email;
  }

  public String getRole() {
    return role;
  }
}
