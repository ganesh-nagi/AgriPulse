package com.agripulse.storage;

import jakarta.validation.constraints.DecimalMin;

public class UpdateOccupancyRequest {

  @DecimalMin("0.0") private double occupiedTonnes;

  public double getOccupiedTonnes() {
    return occupiedTonnes;
  }

  public void setOccupiedTonnes(double occupiedTonnes) {
    this.occupiedTonnes = occupiedTonnes;
  }
}
