package com.agripulse.supply;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Owner edits to their own report. Ranges and window are re-validated. */
public class UpdateSupplyReportRequest {

  @DecimalMin("0.0") private double quantityMinTonnes;

  @DecimalMin("0.0") private double quantityMaxTonnes;

  @NotNull private LocalDate harvestStart;

  @NotNull private LocalDate harvestEnd;

  private String quality;

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
}
