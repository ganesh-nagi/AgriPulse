package com.agripulse.demand;

import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.demand.DemandEstimateResponse.SignalProvenance;
import com.agripulse.market.MarketObservation;
import com.agripulse.market.MarketObservationRepository;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic demand intelligence (no ML). Combines three evidence
 * classes for one crop, region and required-date window:
 *
 * <ol>
 *   <li>CONFIRMED demand = sum of OPEN buyer requirements in the window.</li>
 *   <li>OBSERVED activity = seeded/imported market observations for the
 *       crop and region whose period ended within the 24 months before the
 *       window end (comparable recent seasons). Absorption range is
 *       [min of observed mins, max of observed maxes].</li>
 *   <li>ESTIMATED demand = [confirmed + absorptionMin,
 *       confirmed + absorptionMax].</li>
 * </ol>
 *
 * <p>Confidence weights (fixed, documented): base 20; confirmed demand
 * present +15; two or more distinct buyers +10; observations: 1 obs +15,
 * 2-3 obs +25, 4+ obs +35; all observations SIMULATED -10 (seed discount).
 * Clamped to [5, 95]: never 100, never 0 while inputs exist, so downstream
 * consumers always see an explicit uncertainty band.
 */
@Service
public class DemandEstimationService {

  private final BuyerRequirementRepository requirements;
  private final MarketObservationRepository observations;
  private final DemandEstimateRepository estimates;
  private final CropRepository crops;

  public DemandEstimationService(
      BuyerRequirementRepository requirements,
      MarketObservationRepository observations,
      DemandEstimateRepository estimates,
      CropRepository crops) {
    this.requirements = requirements;
    this.observations = observations;
    this.estimates = estimates;
    this.crops = crops;
  }

  @Transactional
  public DemandEstimateResponse estimate(
      Long cropId, String region, LocalDate windowStart, LocalDate windowEnd) {
    DemandEstimateResponse response = computeUnsaved(cropId, region, windowStart, windowEnd);
    Crop crop =
        crops.findById(cropId).orElseThrow(() -> new IllegalArgumentException("Crop not found"));
    DemandEstimate stored = new DemandEstimate();
    stored.setCrop(crop);
    stored.setRegion(response.getRegion());
    stored.setConfirmedDemandTonnes(response.getConfirmedDemandTonnes());
    stored.setAbsorptionMinTonnes(response.getAbsorptionMinTonnes());
    stored.setAbsorptionMaxTonnes(response.getAbsorptionMaxTonnes());
    stored.setEstimatedMinTonnes(response.getEstimatedMinTonnes());
    stored.setEstimatedMaxTonnes(response.getEstimatedMaxTonnes());
    stored.setConfidence(response.getConfidence());
    stored.setDataSource(DataSource.ESTIMATED);
    estimates.save(stored);
    return response;
  }

  /**
   * Read-only twin of {@link #estimate}: identical math, never persists.
   * Used by the what-if simulator so scenarios leave production state
   * untouched.
   */
  @Transactional(readOnly = true)
  public DemandEstimateResponse computeUnsaved(
      Long cropId, String region, LocalDate windowStart, LocalDate windowEnd) {
    if (windowStart == null || windowEnd == null || windowStart.isAfter(windowEnd)) {
      throw new IllegalArgumentException("Valid windowStart/windowEnd required");
    }
    Crop crop =
        crops.findById(cropId).orElseThrow(() -> new IllegalArgumentException("Crop not found"));
    String normRegion = region == null ? "" : region.trim();

    List<BuyerRequirement> confirmed =
        requirements.findByCropIdAndRegionAndStatusInAndRequiredDateBetween(
            cropId, normRegion, List.of(RequirementStatus.OPEN), windowStart, windowEnd);
    double confirmedTonnes = confirmed.stream().mapToDouble(BuyerRequirement::getQuantityTonnes).sum();
    long buyerCount = confirmed.stream().map(r -> r.getBuyer().getId()).distinct().count();
    Set<DataSource> confirmedSources =
        confirmed.stream().map(BuyerRequirement::getDataSource).collect(Collectors.toSet());

    LocalDate earliestPeriodEnd = windowEnd.minusDays(730);
    List<MarketObservation> relevant =
        observations.findByCropId(cropId).stream()
            .filter(o -> normRegion.equalsIgnoreCase(o.getMarket().getRegion()))
            .filter(o -> !o.getPeriodEnd().isBefore(earliestPeriodEnd))
            .filter(o -> !o.getPeriodEnd().isAfter(windowEnd))
            .toList();
    double absorptionMin =
        relevant.stream().mapToDouble(MarketObservation::getAbsorptionMinTonnes).min().orElse(0.0);
    double absorptionMax =
        relevant.stream().mapToDouble(MarketObservation::getAbsorptionMaxTonnes).max().orElse(0.0);
    Set<DataSource> observationSources =
        relevant.stream().map(MarketObservation::getDataSource).collect(Collectors.toSet());

    double estimatedMin = confirmedTonnes + absorptionMin;
    double estimatedMax = confirmedTonnes + absorptionMax;

    double confidence = 20.0;
    StringBuilder notes = new StringBuilder("base=20");
    if (confirmedTonnes > 0) {
      confidence += 15.0;
      notes.append(";confirmed_present=+15");
    }
    if (buyerCount >= 2) {
      confidence += 10.0;
      notes.append(";multiple_buyers=+10");
    }
    if (relevant.size() == 1) {
      confidence += 15.0;
      notes.append(";observations_1=+15");
    } else if (relevant.size() <= 3 && !relevant.isEmpty()) {
      confidence += 25.0;
      notes.append(";observations_2_3=+25");
    } else if (relevant.size() >= 4) {
      confidence += 35.0;
      notes.append(";observations_4_plus=+35");
    }
    if (!relevant.isEmpty()
        && observationSources.size() == 1
        && observationSources.contains(DataSource.SIMULATED)) {
      confidence -= 10.0;
      notes.append(";simulated_seed_discount=-10");
    }
    confidence = Math.max(5.0, Math.min(95.0, confidence));

    List<SignalProvenance> provenance =
        List.of(
            new SignalProvenance(
                "CONFIRMED",
                confirmedSources.isEmpty() ? Set.of() : EnumSet.copyOf(confirmedSources),
                confirmed.size() + " open requirements from " + buyerCount + " buyers"),
            new SignalProvenance(
                "OBSERVED",
                observationSources.isEmpty() ? Set.of() : EnumSet.copyOf(observationSources),
                relevant.size() + " market observations in comparable seasons"),
            new SignalProvenance(
                "ESTIMATED", Set.of(DataSource.ESTIMATED), "range with confidence " + confidence));

    return new DemandEstimateResponse(
        crop.getName(),
        normRegion,
        windowStart,
        windowEnd,
        confirmedTonnes,
        (int) buyerCount,
        absorptionMin,
        absorptionMax,
        relevant.size(),
        estimatedMin,
        estimatedMax,
        confidence,
        DataSource.ESTIMATED,
        provenance);
  }
}
