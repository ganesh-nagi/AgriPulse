package com.agripulse.transport;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class CreateTransportRequest {

  @DecimalMin("0.0") private double capacityTonnes;

  private LocalDate availableFrom;

  private LocalDate availableTo;

  @NotBlank private String originRegion;

  private String destRegion;

  private Double costPerKm;

  public double getCapacityTonnes() {
    return capacityTonnes;
  }

  public void setCapacityTonnes(double capacityTonnes) {
    this.capacityTonnes = capacityTonnes;
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

  public String getOriginRegion() {
    return originRegion;
  }

  public void setOriginRegion(String originRegion) {
    this.originRegion = originRegion;
  }

  public String getDestRegion() {
    return destRegion;
  }

  public void setDestRegion(String destRegion) {
    this.destRegion = destRegion;
  }

  public Double getCostPerKm() {
    return costPerKm;
  }

  public void setCostPerKm(Double costPerKm) {
    this.costPerKm = costPerKm;
  }
}
