package com.agripulse.fpo;

import com.agripulse.farmer.OnboardingState;
import com.agripulse.user.VerificationStatus;

/**
 * Permitted member information for FPOs. No phone numbers, no coordinates,
 * no individual quantities — only validation-relevant state.
 */
public class FpoMemberView {

  private final Long farmerUserId;
  private final String farmerName;
  private final String region;
  private final VerificationStatus verificationStatus;
  private final OnboardingState onboardingState;
  private final boolean fpoValidated;
  private final long reportCount;

  public FpoMemberView(
      Long farmerUserId,
      String farmerName,
      String region,
      VerificationStatus verificationStatus,
      OnboardingState onboardingState,
      boolean fpoValidated,
      long reportCount) {
    this.farmerUserId = farmerUserId;
    this.farmerName = farmerName;
    this.region = region;
    this.verificationStatus = verificationStatus;
    this.onboardingState = onboardingState;
    this.fpoValidated = fpoValidated;
    this.reportCount = reportCount;
  }

  public Long getFarmerUserId() {
    return farmerUserId;
  }

  public String getFarmerName() {
    return farmerName;
  }

  public String getRegion() {
    return region;
  }

  public VerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public OnboardingState getOnboardingState() {
    return onboardingState;
  }

  public boolean isFpoValidated() {
    return fpoValidated;
  }

  public long getReportCount() {
    return reportCount;
  }
}
