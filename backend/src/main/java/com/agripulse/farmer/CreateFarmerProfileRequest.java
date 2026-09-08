package com.agripulse.farmer;

import jakarta.validation.constraints.NotBlank;

public class CreateFarmerProfileRequest {

  @NotBlank private String fullName;

  private String phone;

  @NotBlank private String region;

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
}
