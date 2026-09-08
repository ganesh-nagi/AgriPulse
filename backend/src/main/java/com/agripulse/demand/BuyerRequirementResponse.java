package com.agripulse.demand;

import java.time.LocalDate;

public class BuyerRequirementResponse {

  private final Long id;
  private final String cropName;
  private final double quantityTonnes;
  private final String quality;
  private final LocalDate requiredDate;
  private final String region;
  private final RequirementStatus status;
  private final DataSource dataSource;

  public BuyerRequirementResponse(
      Long id,
      String cropName,
      double quantityTonnes,
      String quality,
      LocalDate requiredDate,
      String region,
      RequirementStatus status,
      DataSource dataSource) {
    this.id = id;
    this.cropName = cropName;
    this.quantityTonnes = quantityTonnes;
    this.quality = quality;
    this.requiredDate = requiredDate;
    this.region = region;
    this.status = status;
    this.dataSource = dataSource;
  }

  public Long getId() {
    return id;
  }

  public String getCropName() {
    return cropName;
  }

  public double getQuantityTonnes() {
    return quantityTonnes;
  }

  public String getQuality() {
    return quality;
  }

  public LocalDate getRequiredDate() {
    return requiredDate;
  }

  public String getRegion() {
    return region;
  }

  public RequirementStatus getStatus() {
    return status;
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}
