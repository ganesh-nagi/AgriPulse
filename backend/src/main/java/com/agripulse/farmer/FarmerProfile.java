package com.agripulse.farmer;

import com.agripulse.common.BaseEntity;
import com.agripulse.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Farmer account profile. Links a User to farm operations. */
@Entity
@Table(name = "farmer_profiles")
public class FarmerProfile extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(length = 32)
  private String phone;

  @Column(nullable = false)
  private String region;

  @Enumerated(EnumType.STRING)
  @Column(name = "onboarding_state", nullable = false, length = 32)
  private OnboardingState onboardingState = OnboardingState.ACCOUNT_CREATED;

  @Column(name = "fpo_validated", nullable = false)
  private boolean fpoValidated = false;

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public OnboardingState getOnboardingState() {
    return onboardingState;
  }

  public void setOnboardingState(OnboardingState onboardingState) {
    this.onboardingState = onboardingState;
  }

  public boolean isFpoValidated() {
    return fpoValidated;
  }

  public void setFpoValidated(boolean fpoValidated) {
    this.fpoValidated = fpoValidated;
  }
}
