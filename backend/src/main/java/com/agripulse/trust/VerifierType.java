package com.agripulse.trust;

/**
 * Who performed a verification check.
 * MOCK_KYC stands in for a future government KYC integration and
 * must always be labelled as demo/mock in the UI.
 */
public enum VerifierType {
  SELF,
  MOCK_KYC,
  FPO,
  ADMIN
}
