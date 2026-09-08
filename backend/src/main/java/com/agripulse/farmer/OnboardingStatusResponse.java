package com.agripulse.farmer;

import com.agripulse.user.VerificationStatus;

/**
 * Owner-visible verification state. Contains no secrets; OTP codes are only
 * ever returned once, inside the request-phone-verification response.
 */
public class OnboardingStatusResponse {

  private final OnboardingState state;
  private final boolean phoneVerified;
  private final VerificationStatus identityStatus;
  private final boolean fpoValidated;
  private final Boolean regionConsistent;
  private final long evidenceCount;

  public OnboardingStatusResponse(
      OnboardingState state,
      boolean phoneVerified,
      VerificationStatus identityStatus,
      boolean fpoValidated,
      Boolean regionConsistent,
      long evidenceCount) {
    this.state = state;
    this.phoneVerified = phoneVerified;
    this.identityStatus = identityStatus;
    this.fpoValidated = fpoValidated;
    this.regionConsistent = regionConsistent;
    this.evidenceCount = evidenceCount;
  }

  public OnboardingState getState() {
    return state;
  }

  public boolean isPhoneVerified() {
    return phoneVerified;
  }

  public VerificationStatus getIdentityStatus() {
    return identityStatus;
  }

  public boolean isFpoValidated() {
    return fpoValidated;
  }

  public Boolean getRegionConsistent() {
    return regionConsistent;
  }

  public long getEvidenceCount() {
    return evidenceCount;
  }
}
