package com.agripulse.demand;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Regional demand picture for one crop and window. Figures are ranges with
 * an explicit confidence (0-95, never 100) and per-signal provenance. Never
 * exact future demand.
 */
public class DemandEstimateResponse {

  /** One evidence class and where it came from. */
  public record SignalProvenance(String signal, Set<DataSource> sources, String detail) {}

  private final String cropName;
  private final String region;
  private final LocalDate windowStart;
  private final LocalDate windowEnd;
  private final double confirmedDemandTonnes;
  private final int confirmedBuyerCount;
  private final double absorptionMinTonnes;
  private final double absorptionMaxTonnes;
  private final int observationCount;
  private final double estimatedMinTonnes;
  private final double estimatedMaxTonnes;
  private final double confidence;
  private final DataSource dataSource;
  private final List<SignalProvenance> provenance;

  public DemandEstimateResponse(
      String cropName,
      String region,
      LocalDate windowStart,
      LocalDate windowEnd,
      double confirmedDemandTonnes,
      int confirmedBuyerCount,
      double absorptionMinTonnes,
      double absorptionMaxTonnes,
      int observationCount,
      double estimatedMinTonnes,
      double estimatedMaxTonnes,
      double confidence,
      DataSource dataSource,
      List<SignalProvenance> provenance) {
    this.cropName = cropName;
    this.region = region;
    this.windowStart = windowStart;
    this.windowEnd = windowEnd;
    this.confirmedDemandTonnes = confirmedDemandTonnes;
    this.confirmedBuyerCount = confirmedBuyerCount;
    this.absorptionMinTonnes = absorptionMinTonnes;
    this.absorptionMaxTonnes = absorptionMaxTonnes;
    this.observationCount = observationCount;
    this.estimatedMinTonnes = estimatedMinTonnes;
    this.estimatedMaxTonnes = estimatedMaxTonnes;
    this.confidence = confidence;
    this.dataSource = dataSource;
    this.provenance = provenance;
  }

  public String getCropName() {
    return cropName;
  }

  public String getRegion() {
    return region;
  }

  public LocalDate getWindowStart() {
    return windowStart;
  }

  public LocalDate getWindowEnd() {
    return windowEnd;
  }

  public double getConfirmedDemandTonnes() {
    return confirmedDemandTonnes;
  }

  public int getConfirmedBuyerCount() {
    return confirmedBuyerCount;
  }

  public double getAbsorptionMinTonnes() {
    return absorptionMinTonnes;
  }

  public double getAbsorptionMaxTonnes() {
    return absorptionMaxTonnes;
  }

  public int getObservationCount() {
    return observationCount;
  }

  public double getEstimatedMinTonnes() {
    return estimatedMinTonnes;
  }

  public double getEstimatedMaxTonnes() {
    return estimatedMaxTonnes;
  }

  public double getConfidence() {
    return confidence;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public List<SignalProvenance> getProvenance() {
    return provenance;
  }
}
