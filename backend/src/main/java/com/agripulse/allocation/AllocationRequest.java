package com.agripulse.allocation;

import java.time.LocalDate;

/** Surplus placement question: where can this excess quantity feasibly go. */
public class AllocationRequest {

  private Long cropId;
  private String region;
  private LocalDate windowStart;
  private LocalDate windowEnd;
  private double surplusTonnes;

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

  public double getSurplusTonnes() {
    return surplusTonnes;
  }

  public void setSurplusTonnes(double surplusTonnes) {
    this.surplusTonnes = surplusTonnes;
  }
}
