package com.agripulse.user;

/**
 * Farmer/account verification state.
 * Mock/demo providers only for the MVP - never presented as government KYC.
 */
public enum VerificationStatus {
  UNVERIFIED,
  PHONE_VERIFIED,
  IDENTITY_VERIFIED,
  FPO_VALIDATED,
  REJECTED
}
