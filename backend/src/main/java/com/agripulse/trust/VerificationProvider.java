package com.agripulse.trust;

import com.agripulse.user.User;

/**
 * Abstraction over external verification. A real KYC/SMS provider implements
 * this interface later; the workflow states in OnboardingState stay unchanged.
 */
public interface VerificationProvider {

  /** Short provider tag used in references, e.g. "MOCK". */
  String providerName();

  /** Phone check. channelCode is the OTP the real provider would SMS. */
  VerificationResult verifyPhone(User user, String phone);

  /** Identity check. May return PENDING when manual review is needed. */
  VerificationResult verifyIdentity(User user, String fullName, String region);

  /** Completes a pending identity check. */
  VerificationResult confirmIdentity(User user, String reference);
}
