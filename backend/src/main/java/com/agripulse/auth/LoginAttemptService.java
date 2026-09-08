package com.agripulse.auth;

import com.agripulse.audit.AuditService;
import com.agripulse.user.User;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.UserRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records failed logins in its own transaction. This must commit even though
 * the surrounding login call fails and rolls back — otherwise lockout counters
 * and failure audits would silently vanish.
 */
@Service
public class LoginAttemptService {

  private final UserRepository users;
  private final AuditService auditService;
  private final int maxFailedAttempts;
  private final long lockMinutes;

  public LoginAttemptService(
      UserRepository users,
      AuditService auditService,
      @Value("${app.auth.max-failed-attempts:5}") int maxFailedAttempts,
      @Value("${app.auth.lock-minutes:15}") long lockMinutes) {
    this.users = users;
    this.auditService = auditService;
    this.maxFailedAttempts = maxFailedAttempts;
    this.lockMinutes = lockMinutes;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void recordFailure(Long userId, String emailForAudit) {
    if (userId != null) {
      users
          .findById(userId)
          .ifPresent(
              user -> {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= maxFailedAttempts
                    && user.getAccountStatus() == AccountStatus.ACTIVE) {
                  user.setAccountStatus(AccountStatus.LOCKED);
                  user.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                  auditService.record(
                      user.getId(), "ACCOUNT_LOCKED", "User", String.valueOf(user.getId()), null);
                }
              });
      auditService.record(userId, "LOGIN_FAILED", "User", String.valueOf(userId), null);
    } else {
      auditService.record(null, "LOGIN_FAILED", "User", emailForAudit, null);
    }
  }
}
