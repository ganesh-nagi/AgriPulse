package com.agripulse.fpo;

import jakarta.validation.constraints.NotNull;

public class ValidateFarmerRequest {

  @NotNull private Long farmerUserId;

  private boolean approved = true;

  private String notes;

  public Long getFarmerUserId() {
    return farmerUserId;
  }

  public void setFarmerUserId(Long farmerUserId) {
    this.farmerUserId = farmerUserId;
  }

  public boolean isApproved() {
    return approved;
  }

  public void setApproved(boolean approved) {
    this.approved = approved;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
