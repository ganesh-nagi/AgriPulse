package com.agripulse.demand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateBuyerRequirementRequest {

  @NotNull private Long cropId;

  @DecimalMin("0.0") private double quantityTonnes;

  private String quality;

  @NotNull private LocalDate requiredDate;

  @NotBlank private String region;

  public Long getCropId() {
    return cropId;
  }

  public void setCropId(Long cropId) {
    this.cropId = cropId;
  }

  public double getQuantityTonnes() {
    return quantityTonnes;
  }

  public void setQuantityTonnes(double quantityTonnes) {
    this.quantityTonnes = quantityTonnes;
  }

  public String getQuality() {
    return quality;
  }

  public void setQuality(String quality) {
    this.quality = quality;
  }

  public LocalDate getRequiredDate() {
    return requiredDate;
  }

  public void setRequiredDate(LocalDate requiredDate) {
    this.requiredDate = requiredDate;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }
}
