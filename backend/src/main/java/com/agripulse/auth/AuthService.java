package com.agripulse.auth;

import com.agripulse.audit.AuditService;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login, refresh rotation and logout. Passwords are BCrypt-hashed
 * and server-validated; ADMIN can never be self-registered; roles always come
 * from the stored account, never from client input. Audit logs carry ids and
 * outcomes only — never passwords or tokens.
 */
@Service
public class AuthService {

  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final TokenDenylist tokenDenylist;
  private final AuditService auditService;
  private final long refreshExpirationMs;
  private final int maxFailedAttempts;
  private final long lockMinutes;

  public AuthService(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      TokenDenylist tokenDenylist,
      AuditService auditService,
      @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs,
      @Value("${app.auth.max-failed-attempts:5}") int maxFailedAttempts,
      @Value("${app.auth.lock-minutes:15}") long lockMinutes) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.tokenDenylist = tokenDenylist;
    this.auditService = auditService;
    this.refreshExpirationMs = refreshExpirationMs;
    this.maxFailedAttempts = maxFailedAttempts;
    this.lockMinutes = lockMinutes;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (request.getRole() == Role.ADMIN) {
      throw new AccessDeniedException("ADMIN role cannot be self-registered");
    }
    PasswordPolicy.validate(request.getPassword());
    String email = request.getEmail().trim().toLowerCase();
    if (users.existsByEmail(email)) {
      throw new IllegalArgumentException("Email is already registered");
    }
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setRole(request.getRole());
    user.setAccountStatus(AccountStatus.ACTIVE);
    users.save(user);
    String refreshToken = issueRefreshToken(user);
    auditService.record(
        user.getId(),
        "USER_REGISTERED",
        "User",
        String.valueOf(user.getId()),
        "role=" + user.getRole().name());
    return toResponse(user, refreshToken);
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    String email = request.getEmail().trim().toLowerCase();
    User user = users.findByEmail(email).orElse(null);
    if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      if (user != null) {
        registerFailedAttempt(user);
      }
      auditService.record(null, "LOGIN_FAILED", "User", email, null);
      throw new IllegalArgumentException("Invalid email or password");
    }
    ensureUsable(user);
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    String refreshToken = issueRefreshToken(user);
    auditService.record(user.getId(), "USER_LOGIN", "User", String.valueOf(user.getId()), null);
    return toResponse(user, refreshToken);
  }

  /** Rotation: the presented refresh token is revoked and a new pair is issued. */
  @Transactional
  public AuthResponse refresh(String rawRefreshToken) {
    RefreshToken stored =
        refreshTokens
            .findByTokenHash(sha256(rawRefreshToken))
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
    if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("Invalid refresh token");
    }
    ensureUsable(stored.getUser());
    stored.setRevoked(true);
    String next = issueRefreshToken(stored.getUser());
    auditService.record(
        stored.getUser().getId(),
        "TOKEN_REFRESHED",
        "User",
        String.valueOf(stored.getUser().getId()),
        null);
    return toResponse(stored.getUser(), next);
  }

  /**
   * Logout: revokes all refresh tokens and denylists the current access token
   * so it cannot be reused before expiry.
   */
  @Transactional
  public void logout(String userEmail, String accessToken) {
    User user =
        users.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    refreshTokens.findByUserIdAndRevokedFalse(user.getId()).forEach(t -> t.setRevoked(true));
    jwtService
        .validateAndExtractIdentity(accessToken)
        .ifPresent(
            identity ->
                tokenDenylist.deny(
                    identity.jti(), System.currentTimeMillis() + 86_400_000L));
    auditService.record(
        user.getId(), "USER_LOGOUT", "User", String.valueOf(user.getId()), null);
  }

  /** ADMIN-only role change (controller also enforces @PreAuthorize). */
  @Transactional
  public void changeRole(Long userId, Role newRole) {
    User user =
        users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    Role old = user.getRole();
    user.setRole(newRole);
    auditService.record(
        null,
        "ROLE_CHANGED",
        "User",
        String.valueOf(user.getId()),
        "role=" + old.name() + "->" + newRole.name());
  }

  private void ensureUsable(User user) {
    if (user.getAccountStatus() == AccountStatus.DISABLED) {
      throw new AccountLockedException("Account is disabled");
    }
    if (user.getAccountStatus() == AccountStatus.LOCKED
        && (user.getLockedUntil() == null || user.getLockedUntil().isAfter(LocalDateTime.now()))) {
      throw new AccountLockedException("Account is temporarily locked");
    }
    if (user.getAccountStatus() == AccountStatus.LOCKED) {
      user.setAccountStatus(AccountStatus.ACTIVE);
      user.setFailedLoginAttempts(0);
      user.setLockedUntil(null);
    }
  }

  private void registerFailedAttempt(User user) {
    int attempts = user.getFailedLoginAttempts() + 1;
    user.setFailedLoginAttempts(attempts);
    if (attempts >= maxFailedAttempts) {
      user.setAccountStatus(AccountStatus.LOCKED);
      user.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
      auditService.record(
          user.getId(), "ACCOUNT_LOCKED", "User", String.valueOf(user.getId()), null);
    }
  }

  private String issueRefreshToken(User user) {
    byte[] random = new byte[32];
    new SecureRandom().nextBytes(random);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    RefreshToken stored = new RefreshToken();
    stored.setUser(user);
    stored.setTokenHash(sha256(raw));
    stored.setExpiresAt(
        LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000L));
    refreshTokens.save(stored);
    return raw;
  }

  private AuthResponse toResponse(User user, String refreshToken) {
    return new AuthResponse(
        jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name()),
        refreshToken,
        user.getEmail(),
        user.getRole().name());
  }

  public static String sha256(String raw) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (byte b : digest) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }
}
