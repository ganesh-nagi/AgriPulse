package com.agripulse.user;

import com.agripulse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Login account. Exactly one role per user for MVP simplicity.
 * Secrets: only a BCrypt hash is stored, never the plain password.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Role role;

  @Column(length = 32)
  private String phone;

  @Column(name = "phone_verified", nullable = false)
  private boolean phoneVerified = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false, length = 32)
  private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_status", nullable = false, length = 16)
  private AccountStatus accountStatus = AccountStatus.ACTIVE;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts = 0;

  @Column(name = "locked_until")
  private java.time.LocalDateTime lockedUntil;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public boolean isPhoneVerified() {
    return phoneVerified;
  }

  public void setPhoneVerified(boolean phoneVerified) {
    this.phoneVerified = phoneVerified;
  }

  public VerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public void setVerificationStatus(VerificationStatus verificationStatus) {
    this.verificationStatus = verificationStatus;
  }

  public AccountStatus getAccountStatus() {
    return accountStatus;
  }

  public void setAccountStatus(AccountStatus accountStatus) {
    this.accountStatus = accountStatus;
  }

  public int getFailedLoginAttempts() {
    return failedLoginAttempts;
  }

  public void setFailedLoginAttempts(int failedLoginAttempts) {
    this.failedLoginAttempts = failedLoginAttempts;
  }

  public java.time.LocalDateTime getLockedUntil() {
    return lockedUntil;
  }

  public void setLockedUntil(java.time.LocalDateTime lockedUntil) {
    this.lockedUntil = lockedUntil;
  }
}
