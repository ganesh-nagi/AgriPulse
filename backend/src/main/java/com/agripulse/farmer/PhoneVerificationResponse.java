package com.agripulse.farmer;

/**
 * Returned once when phone verification is requested. The demo code is shown
 * here only because the MOCK provider cannot send SMS; a real provider
 * response carries no code.
 */
public class PhoneVerificationResponse {

  private final OnboardingState state;
  private final String demoCode;
  private final String note;

  public PhoneVerificationResponse(OnboardingState state, String demoCode, String note) {
    this.state = state;
    this.demoCode = demoCode;
    this.note = note;
  }

  public OnboardingState getState() {
    return state;
  }

  public String getDemoCode() {
    return demoCode;
  }

  public String getNote() {
    return note;
  }
}
