package com.agripulse.farmer;

/**
 * Farmer verification workflow, in order. A real KYC provider can be plugged
 * behind VerificationProvider later; the states stay the same.
 */
public enum OnboardingState {
  ACCOUNT_CREATED,
  PHONE_VERIFICATION_PENDING,
  PHONE_VERIFIED,
  IDENTITY_VERIFICATION_PENDING,
  IDENTITY_VERIFIED,
  REQUIRES_REVIEW,
  FARM_REGION_VERIFICATION,
  PROFILE_ACTIVE
}
