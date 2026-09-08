package com.agripulse.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JWT creation and validation (jjwt, HS256).
 * The secret must be at least 32 bytes; the app fails fast at startup otherwise.
 */
@Service
public class JwtService {

  private final SecretKey key;
  private final long expirationMs;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalStateException("JWT secret must be at least 32 bytes long");
    }
    this.key = Keys.hmacShaKeyFor(bytes);
    this.expirationMs = expirationMs;
  }

  public String generateToken(Long userId, String email, String role) {
    Date now = new Date();
    return Jwts.builder()
        .subject(email)
        .id(java.util.UUID.randomUUID().toString())
        .claim("uid", userId)
        .claim("role", role)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expirationMs))
        .signWith(key)
        .compact();
  }

  /** Validated token identity: subject email, token id and expiry (for logout). */
  public record TokenIdentity(String email, String jti, long expiresAtMs) {}

  /** Returns the identity when the token is valid, empty otherwise. */
  public Optional<TokenIdentity> validateAndExtractIdentity(String token) {
    try {
      var payload = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      if (payload.getSubject() == null
          || payload.getId() == null
          || payload.getExpiration() == null) {
        return Optional.empty();
      }
      return Optional.of(
          new TokenIdentity(
              payload.getSubject(), payload.getId(), payload.getExpiration().getTime()));
    } catch (JwtException | IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  /** Returns the subject email when the token is valid, empty otherwise. */
  public Optional<String> validateAndExtractEmail(String token) {
    return validateAndExtractIdentity(token).map(TokenIdentity::email);
  }
}
