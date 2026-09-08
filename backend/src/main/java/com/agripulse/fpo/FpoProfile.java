package com.agripulse.fpo;

import com.agripulse.common.BaseEntity;
import com.agripulse.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Farmer Producer Organisation profile. FPOs validate farmer reports. */
@Entity
@Table(name = "fpo_profiles")
public class FpoProfile extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "fpo_name", nullable = false)
  private String fpoName;

  @Column(nullable = false)
  private String region;

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getFpoName() {
    return fpoName;
  }

  public void setFpoName(String fpoName) {
    this.fpoName = fpoName;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }
}
