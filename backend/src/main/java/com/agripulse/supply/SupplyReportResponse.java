package com.agripulse.supply;

import java.time.LocalDate;

/**
 * Owner view of a supply report. Regional/aggregate views use a separate
 * DTO without coordinates or reporter identity (privacy principle).
 */
public class SupplyReportResponse {

  private final Long id;
  private final Long farmId;
  private final String cropName;
  private final double quantityMinTonnes;
  private final double quantityMaxTonnes;
  private final LocalDate harvestStart;
  private final LocalDate harvestEnd;
  private final String quality;
  private final String region;
  private final ReportStatus status;
  private final double trustScore;
  private final String trustLevel;

  public SupplyReportResponse(
      Long id,
      Long farmId,
      String cropName,
      double quantityMinTonnes,
      double quantityMaxTonnes,
      LocalDate harvestStart,
      LocalDate harvestEnd,
      String quality,
      String region,
      ReportStatus status,
      double trustScore,
      String trustLevel) {
    this.id = id;
    this.farmId = farmId;
    this.cropName = cropName;
    this.quantityMinTonnes = quantityMinTonnes;
    this.quantityMaxTonnes = quantityMaxTonnes;
    this.harvestStart = harvestStart;
    this.harvestEnd = harvestEnd;
    this.quality = quality;
    this.region = region;
    this.status = status;
    this.trustScore = trustScore;
    this.trustLevel = trustLevel;
  }

  public Long getId() {
    return id;
  }

  public Long getFarmId() {
    return farmId;
  }

  public String getCropName() {
    return cropName;
  }

  public double getQuantityMinTonnes() {
    return quantityMinTonnes;
  }

  public double getQuantityMaxTonnes() {
    return quantityMaxTonnes;
  }

  public LocalDate getHarvestStart() {
    return harvestStart;
  }

  public LocalDate getHarvestEnd() {
    return harvestEnd;
  }

  public String getQuality() {
    return quality;
  }

  public String getRegion() {
    return region;
  }

  public ReportStatus getStatus() {
    return status;
  }

  public double getTrustScore() {
    return trustScore;
  }

  public String getTrustLevel() {
    return trustLevel;
  }
}
