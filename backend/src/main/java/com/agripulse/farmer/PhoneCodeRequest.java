package com.agripulse.farmer;

import jakarta.validation.constraints.NotBlank;

public class PhoneCodeRequest {

  @NotBlank private String code;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
