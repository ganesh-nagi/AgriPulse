package com.agripulse.storage;

public class StorageResponse {

  private final Long id;
  private final String name;
  private final String region;
  private final double capacityTonnes;
  private final double occupiedTonnes;
  private final double availableTonnes;

  public StorageResponse(
      Long id,
      String name,
      String region,
      double capacityTonnes,
      double occupiedTonnes,
      double availableTonnes) {
    this.id = id;
    this.name = name;
    this.region = region;
    this.capacityTonnes = capacityTonnes;
    this.occupiedTonnes = occupiedTonnes;
    this.availableTonnes = availableTonnes;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getRegion() {
    return region;
  }

  public double getCapacityTonnes() {
    return capacityTonnes;
  }

  public double getOccupiedTonnes() {
    return occupiedTonnes;
  }

  public double getAvailableTonnes() {
    return availableTonnes;
  }
}
