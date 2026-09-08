package com.agripulse.supply;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Farmer supply signal. Quantities are min/max ranges in tonnes. */
public class CreateSupplyReportRequest {

  @NotNull private Long farmId;

  @NotNull private Long cropId;

  @DecimalMin("0.0") private double quantityMinTonnes;

  @DecimalMin("0.0") private double quantityMaxTonnes;

  @NotNull private LocalDate harvestStart;

  @NotNull private LocalDate harvestEnd;

  private String quality;

  @NotBlank private String region;

  private Double latitude;

  private Double longitude;

  public Long getFarmId() {
    return farmId;
  }

  public void setFarmId(Long farmId) {
    this.farmId = farmId;
  }

  public Long getCropId() {
    return cropId;
  }

  public void setCropId(Long cropId) {
    this.cropId = cropId;
  }

  public double getQuantityMinTonnes() {
    return quantityMinTonnes;
  }

  public void setQuantityMinTonnes(double quantityMinTonnes) {
    this.quantityMinTonnes = quantityMinTonnes;
  }

  public double getQuantityMaxTonnes() {
    return quantityMaxTonnes;
  }

  public void setQuantityMaxTonnes(double quantityMaxTonnes) {
    this.quantityMaxTonnes = quantityMaxTonnes;
  }

  public LocalDate getHarvestStart() {
    return harvestStart;
  }

  public void setHarvestStart(LocalDate harvestStart) {
    this.harvestStart = harvestStart;
  }

  public LocalDate getHarvestEnd() {
    return harvestEnd;
  }

  public void setHarvestEnd(LocalDate harvestEnd) {
    this.harvestEnd = harvestEnd;
  }

  public String getQuality() {
    return quality;
  }

  public void setQuality(String quality) {
    this.quality = quality;
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
}
