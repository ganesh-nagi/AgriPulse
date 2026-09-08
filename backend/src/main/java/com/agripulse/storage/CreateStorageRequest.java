package com.agripulse.storage;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateStorageRequest {

  @NotBlank private String name;

  @NotBlank private String region;

  @DecimalMin("0.0") private double capacityTonnes;

  private String cropCompatibility;

  private Double costPerDay;

  private LocalDate availableFrom;

  private LocalDate availableTo;

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

  public double getCapacityTonnes() {
    return capacityTonnes;
  }

  public void setCapacityTonnes(double capacityTonnes) {
    this.capacityTonnes = capacityTonnes;
  }

  public String getCropCompatibility() {
    return cropCompatibility;
  }

  public void setCropCompatibility(String cropCompatibility) {
    this.cropCompatibility = cropCompatibility;
  }

  public Double getCostPerDay() {
    return costPerDay;
  }

  public void setCostPerDay(Double costPerDay) {
    this.costPerDay = costPerDay;
  }

  public LocalDate getAvailableFrom() {
    return availableFrom;
  }

  public void setAvailableFrom(LocalDate availableFrom) {
    this.availableFrom = availableFrom;
  }

  public LocalDate getAvailableTo() {
    return availableTo;
  }

  public void setAvailableTo(LocalDate availableTo) {
    this.availableTo = availableTo;
  }
}
