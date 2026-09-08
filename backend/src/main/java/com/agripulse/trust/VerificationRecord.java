package com.agripulse.trust;

import com.agripulse.common.BaseEntity;
import com.agripulse.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One verification check against a user (phone, identity, farm, FPO validation). */
@Entity
@Table(name = "verification_records")
public class VerificationRecord extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "subject_user_id", nullable = false)
  private User subject;

  @Enumerated(EnumType.STRING)
  @Column(name = "verifier_type", nullable = false, length = 16)
  private VerifierType verifierType;

  @Column(nullable = false, length = 64)
  private String method;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private VerificationDecision decision = VerificationDecision.PENDING;

  @Column(length = 1024)
  private String notes;

  /** SHA-256 of a one-time code/case secret. The raw secret is never stored. */
  @Column(name = "secret_hash", length = 64)
  private String secretHash;

  public User getSubject() {
    return subject;
  }

  public void setSubject(User subject) {
    this.subject = subject;
  }

  public VerifierType getVerifierType() {
    return verifierType;
  }

  public void setVerifierType(VerifierType verifierType) {
    this.verifierType = verifierType;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public VerificationDecision getDecision() {
    return decision;
  }

  public void setDecision(VerificationDecision decision) {
    this.decision = decision;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getSecretHash() {
    return secretHash;
  }

  public void setSecretHash(String secretHash) {
    this.secretHash = secretHash;
  }
}
