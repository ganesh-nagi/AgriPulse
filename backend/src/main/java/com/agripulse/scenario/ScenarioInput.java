package com.agripulse.scenario;

import java.time.LocalDate;

/**
 * One what-if question. Deltas shift supply/demand; overrides replace a
 * resource availability figure (null = keep production state). Overrides
 * must be &gt;= 0; deltas may be positive or negative.
 */
public class ScenarioInput {

  private Long cropId;
  private String region;
  private LocalDate windowStart;
  private LocalDate windowEnd;
  private double supplyDeltaTonnes;
  private double demandDeltaTonnes;
  private Double storageOverrideTonnes;
  private Double transportOverrideTonnes;
  private Double processingOverrideTonnes;

  public Long getCropId() {
    return cropId;
  }

  public void setCropId(Long cropId) {
    this.cropId = cropId;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public LocalDate getWindowStart() {
    return windowStart;
  }

  public void setWindowStart(LocalDate windowStart) {
    this.windowStart = windowStart;
  }

  public LocalDate getWindowEnd() {
    return windowEnd;
  }

  public void setWindowEnd(LocalDate windowEnd) {
    this.windowEnd = windowEnd;
  }

  public double getSupplyDeltaTonnes() {
    return supplyDeltaTonnes;
  }

  public void setSupplyDeltaTonnes(double supplyDeltaTonnes) {
    this.supplyDeltaTonnes = supplyDeltaTonnes;
  }

  public double getDemandDeltaTonnes() {
    return demandDeltaTonnes;
  }

  public void setDemandDeltaTonnes(double demandDeltaTonnes) {
    this.demandDeltaTonnes = demandDeltaTonnes;
  }

  public Double getStorageOverrideTonnes() {
    return storageOverrideTonnes;
  }

  public void setStorageOverrideTonnes(Double storageOverrideTonnes) {
    this.storageOverrideTonnes = storageOverrideTonnes;
  }

  public Double getTransportOverrideTonnes() {
    return transportOverrideTonnes;
  }

  public void setTransportOverrideTonnes(Double transportOverrideTonnes) {
    this.transportOverrideTonnes = transportOverrideTonnes;
  }

  public Double getProcessingOverrideTonnes() {
    return processingOverrideTonnes;
  }

  public void setProcessingOverrideTonnes(Double processingOverrideTonnes) {
    this.processingOverrideTonnes = processingOverrideTonnes;
  }
}
