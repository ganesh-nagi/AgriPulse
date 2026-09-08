package com.agripulse.allocation;

import java.util.List;

/**
 * Surplus placement outcome. Remaining exposure is reported honestly:
 * a non-zero value means capacity is insufficient, not a failure.
 */
public class AllocationResult {

  private final double surplusTonnes;
  private final List<AllocationEntry> entries;
  private final double totalAllocatedTonnes;
  private final double remainingExposureTonnes;
  private final boolean fullyAbsorbed;

  public AllocationResult(
      double surplusTonnes,
      List<AllocationEntry> entries,
      double totalAllocatedTonnes,
      double remainingExposureTonnes,
      boolean fullyAbsorbed) {
    this.surplusTonnes = surplusTonnes;
    this.entries = entries;
    this.totalAllocatedTonnes = totalAllocatedTonnes;
    this.remainingExposureTonnes = remainingExposureTonnes;
    this.fullyAbsorbed = fullyAbsorbed;
  }

  public double getSurplusTonnes() {
    return surplusTonnes;
  }

  public List<AllocationEntry> getEntries() {
    return entries;
  }

  public double getTotalAllocatedTonnes() {
    return totalAllocatedTonnes;
  }

  public double getRemainingExposureTonnes() {
    return remainingExposureTonnes;
  }

  public boolean isFullyAbsorbed() {
    return fullyAbsorbed;
  }
}
