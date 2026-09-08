package com.agripulse.farmer;

import com.agripulse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A farmer's farm. Coordinates are approximate and stored server-side only;
 * public APIs must never expose exact farm location.
 */
@Entity
@Table(name = "farms")
public class Farm extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "farmer_profile_id", nullable = false)
  private FarmerProfile farmer;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String region;

  /** Approximate latitude, nullable. Never exposed publicly. */
  private Double latitude;

  /** Approximate longitude, nullable. Never exposed publicly. */
  private Double longitude;

  @Column(name = "area_acres")
  private Double areaAcres;

  public FarmerProfile getFarmer() {
    return farmer;
  }

  public void setFarmer(FarmerProfile farmer) {
    this.farmer = farmer;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public Double getAreaAcres() {
    return areaAcres;
  }

  public void setAreaAcres(Double areaAcres) {
    this.areaAcres = areaAcres;
  }
}
