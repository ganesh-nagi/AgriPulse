package com.agripulse.allocation;

/** One placed quantity: target, amount, why, and a cost estimate when rate data exists. */
public class AllocationEntry {

  private final String resourceType;
  private final Long resourceId;
  private final String resourceName;
  private final double allocatedTonnes;
  private final String reason;
  private final Double costEstimate;

  public AllocationEntry(
      String resourceType,
      Long resourceId,
      String resourceName,
      double allocatedTonnes,
      String reason,
      Double costEstimate) {
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.resourceName = resourceName;
    this.allocatedTonnes = allocatedTonnes;
    this.reason = reason;
    this.costEstimate = costEstimate;
  }

  public String getResourceType() {
    return resourceType;
  }

  public Long getResourceId() {
    return resourceId;
  }

  public String getResourceName() {
    return resourceName;
  }

  public double getAllocatedTonnes() {
    return allocatedTonnes;
  }

  public String getReason() {
    return reason;
  }

  public Double getCostEstimate() {
    return costEstimate;
  }
}
