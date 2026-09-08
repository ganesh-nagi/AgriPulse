package com.agripulse.user;

import jakarta.validation.constraints.NotNull;

public class ChangeStatusRequest {

  @NotNull private AccountStatus status;

  public AccountStatus getStatus() {
    return status;
  }

  public void setStatus(AccountStatus status) {
    this.status = status;
  }
}
