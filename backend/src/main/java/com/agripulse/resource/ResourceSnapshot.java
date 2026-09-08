package com.agripulse.resource;

import java.time.LocalDate;

/**
 * Point-in-time resource state for one crop, region and date. Read-only
 * state for later allocation logic; this service never optimizes or books.
 * Market absorption capacity is reported separately from demand estimates.
 */
public class ResourceSnapshot {

  private final String region;
  private final String cropName;
  private final LocalDate date;
  private final double storageAvailableTonnes;
  private final double transportAvailableTonnes;
  private final double processingAvailableTonnes;
  private final double marketAbsorptionMinTonnes;
  private final double marketAbsorptionMaxTonnes;

  public ResourceSnapshot(
      String region,
      String cropName,
      LocalDate date,
      double storageAvailableTonnes,
      double transportAvailableTonnes,
      double processingAvailableTonnes,
      double marketAbsorptionMinTonnes,
      double marketAbsorptionMaxTonnes) {
    this.region = region;
    this.cropName = cropName;
    this.date = date;
    this.storageAvailableTonnes = storageAvailableTonnes;
    this.transportAvailableTonnes = transportAvailableTonnes;
    this.processingAvailableTonnes = processingAvailableTonnes;
    this.marketAbsorptionMinTonnes = marketAbsorptionMinTonnes;
    this.marketAbsorptionMaxTonnes = marketAbsorptionMaxTonnes;
  }

  public String getRegion() {
    return region;
  }

  public String getCropName() {
    return cropName;
  }

  public LocalDate getDate() {
    return date;
  }

  public double getStorageAvailableTonnes() {
    return storageAvailableTonnes;
  }

  public double getTransportAvailableTonnes() {
    return transportAvailableTonnes;
  }

  public double getProcessingAvailableTonnes() {
    return processingAvailableTonnes;
  }

  public double getMarketAbsorptionMinTonnes() {
    return marketAbsorptionMinTonnes;
  }

  public double getMarketAbsorptionMaxTonnes() {
    return marketAbsorptionMaxTonnes;
  }
}
