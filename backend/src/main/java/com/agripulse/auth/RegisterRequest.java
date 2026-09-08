package com.agripulse.auth;

import com.agripulse.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Self-registration payload. ADMIN cannot be self-assigned (rejected server-side). */
public class RegisterRequest {

  @Email @NotBlank private String email;

  @NotBlank
  @Size(min = 8, max = 100)
  private String password;

  @NotNull private Role role;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }
}
