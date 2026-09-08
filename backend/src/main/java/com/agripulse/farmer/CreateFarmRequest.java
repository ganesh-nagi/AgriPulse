package com.agripulse.farmer;

import jakarta.validation.constraints.NotBlank;

public class CreateFarmRequest {

  @NotBlank private String name;

  @NotBlank private String region;

  private Double latitude;

  private Double longitude;

  private Double areaAcres;

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
