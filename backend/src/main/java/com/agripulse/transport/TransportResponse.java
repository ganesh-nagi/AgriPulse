package com.agripulse.transport;

public class TransportResponse {

  private final Long id;
  private final double capacityTonnes;
  private final String originRegion;
  private final String destRegion;
  private final TransportStatus status;

  public TransportResponse(
      Long id,
      double capacityTonnes,
      String originRegion,
      String destRegion,
      TransportStatus status) {
    this.id = id;
    this.capacityTonnes = capacityTonnes;
    this.originRegion = originRegion;
    this.destRegion = destRegion;
    this.status = status;
  }

  public Long getId() {
    return id;
  }

  public double getCapacityTonnes() {
    return capacityTonnes;
  }

  public String getOriginRegion() {
    return originRegion;
  }

  public String getDestRegion() {
    return destRegion;
  }

  public TransportStatus getStatus() {
    return status;
  }
}
