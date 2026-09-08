package com.agripulse.pressure;

import com.agripulse.crop.CropRepository;
import com.agripulse.demand.DemandEstimateResponse;
import com.agripulse.demand.DemandEstimationService;
import com.agripulse.resource.ResourceSnapshot;
import com.agripulse.resource.ResourceStateService;
import com.agripulse.supply.ReportStatus;
import com.agripulse.supply.SupplyReportRepository;
import com.agripulse.trust.TrustScoreRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic market pressure: how estimated peak supply compares to
 * estimated peak absorption. Pure function, fully testable, no AI.
 *
 * <p>The network assessment below combines trusted (confidence-weighted)
 * supply, the demand estimate range, and storage/transport/processing/market
 * state into one band plus reasons. It is a network condition indicator,
 * never an exact prediction of future price.
 *
 * <h2>Assessment rules (fixed, documented)</h2>
 * <ul>
 *   <li>Effective supply = sum over SUBMITTED/VALIDATED reports of
 *       midpoint x confidence/100. Low confidence shrinks influence.</li>
 *   <li>Supply pressure: effective below estimated min = LOW (shortage);
 *       inside the range = MODERATE; above max but within 20% = HIGH;
 *       20%+ above max = CRITICAL.</li>
 *   <li>Storage (optional buffer): coverage of effective supply &gt;= 1 =
 *       LOW; &gt;= 0.25 = MODERATE; &gt; 0 = HIGH (limited); zero =
 *       MODERATE with a must-move-fresh note (absence alone never
 *       escalates).</li>
 *   <li>Transport (must move the harvest): coverage &gt;= 1 = LOW;
 *       &gt;= 0.5 = MODERATE; &gt; 0 = HIGH (constrained); zero = HIGH.</li>
 *   <li>Processing is context only: a coverage note, never a band change.</li>
 *   <li>Demand strength is context, except weak confirmed demand
 *       (&lt; 30% of estimated max) with supply at MODERATE or above
 *       escalates one band (cap CRITICAL).</li>
 *   <li>Overall = highest component band. Zero expected supply forces LOW.</li>
 * </ul>
 */
@Service
public class PressureService {

  /** Pressure outcome with the ratio that produced it and an explanation. */
  public record PressureResult(double ratio, PressureBand band, String explanation) {}

  /** One contributing reason: which component and why. */
  public record PressureReason(String component, String message) {}

  /** Full network assessment: overall band plus every contributing reason. */
  public record PressureAssessment(
      PressureBand band,
      double effectiveSupplyTonnes,
      double estimatedMinTonnes,
      double estimatedMaxTonnes,
      List<PressureReason> reasons) {}

  // Field injection keeps the no-arg constructor working for the pure
  // compute() unit tests; Spring wires these on the managed bean.
  @Autowired private SupplyReportRepository reports;
  @Autowired private TrustScoreRepository trustScores;
  @Autowired private DemandEstimationService demandEstimation;
  @Autowired private ResourceStateService resourceState;
  @Autowired private CropRepository crops;

  public PressureResult compute(double supplyMaxTonnes, double absorptionMaxTonnes) {
    double demand = Math.max(absorptionMaxTonnes, 0.000001);
    double ratio = Math.max(supplyMaxTonnes, 0.0) / demand;
    PressureBand band;
    if (ratio < 0.8) {
      band = PressureBand.LOW;
    } else if (ratio < 1.0) {
      band = PressureBand.MODERATE;
    } else if (ratio < 1.2) {
      band = PressureBand.HIGH;
    } else {
      band = PressureBand.CRITICAL;
    }
    String explanation =
        "Estimated peak supply %.1f t vs estimated peak absorption %.1f t (ratio %.2f)."
            .formatted(Math.max(supplyMaxTonnes, 0.0), Math.max(absorptionMaxTonnes, 0.0), ratio);
    return new PressureResult(ratio, band, explanation);
  }

  @Transactional(readOnly = true)
  public PressureAssessment assess(
      Long cropId, String region, LocalDate windowStart, LocalDate windowEnd) {
    String cropName =
        crops.findById(cropId).orElseThrow(() -> new IllegalArgumentException("Crop not found"))
            .getName();
    String normRegion = region == null ? "" : region.trim();

    double effective = effectiveSupply(cropId, normRegion);
    DemandEstimateResponse demand =
        demandEstimation.estimate(cropId, normRegion, windowStart, windowEnd);
    ResourceSnapshot snapshot = resourceState.snapshot(normRegion, cropId, cropName, windowEnd);
    return evaluate(effective, demand, snapshot);
  }

  /**
   * Pure band-and-reasons evaluation over already-gathered inputs. No
   * database access: production reads and what-if scenarios share this
   * exact logic.
   */
  public PressureAssessment evaluate(
      double effective, DemandEstimateResponse demand, ResourceSnapshot snapshot) {
    List<PressureReason> reasons = new ArrayList<>();
    if (effective <= 0) {
      reasons.add(new PressureReason("SUPPLY", "no expected supply in this window"));
      return new PressureAssessment(
          PressureBand.LOW, 0.0, demand.getEstimatedMinTonnes(), demand.getEstimatedMaxTonnes(),
          reasons);
    }

    PressureBand supplyBand;
    double estMin = demand.getEstimatedMinTonnes();
    double estMax = Math.max(demand.getEstimatedMaxTonnes(), 0.000001);
    if (effective < estMin) {
      supplyBand = PressureBand.LOW;
      reasons.add(
          new PressureReason(
              "SUPPLY",
              "expected supply %.1f t is below estimated demand range %.1f-%.1f t (shortage)"
                  .formatted(effective, estMin, demand.getEstimatedMaxTonnes())));
    } else if (effective <= demand.getEstimatedMaxTonnes()) {
      supplyBand = PressureBand.MODERATE;
      reasons.add(
          new PressureReason(
              "SUPPLY",
              "expected supply %.1f t is within estimated demand range %.1f-%.1f t"
                  .formatted(effective, estMin, demand.getEstimatedMaxTonnes())));
    } else if (effective < estMax * 1.2) {
      supplyBand = PressureBand.HIGH;
      reasons.add(
          new PressureReason(
              "SUPPLY",
              "expected supply %.1f t is above estimated demand range %.1f-%.1f t"
                  .formatted(effective, estMin, demand.getEstimatedMaxTonnes())));
    } else {
      supplyBand = PressureBand.CRITICAL;
      reasons.add(
          new PressureReason(
              "SUPPLY",
              "expected supply %.1f t is 20%%+ above estimated demand range %.1f-%.1f t"
                  .formatted(effective, estMin, demand.getEstimatedMaxTonnes())));
    }

    PressureBand storageBand = PressureBand.LOW;
    double storageCoverage = snapshot.getStorageAvailableTonnes() / effective;
    if (snapshot.getStorageAvailableTonnes() <= 0) {
      storageBand = PressureBand.MODERATE;
      reasons.add(
          new PressureReason("STORAGE", "no storage available; supply must move fresh to market"));
    } else if (storageCoverage < 0.25) {
      storageBand = PressureBand.HIGH;
      reasons.add(
          new PressureReason(
              "STORAGE",
              "storage capacity is limited (covers %.0f%% of expected supply)"
                  .formatted(storageCoverage * 100)));
    } else if (storageCoverage < 1.0) {
      storageBand = PressureBand.MODERATE;
      reasons.add(
          new PressureReason("STORAGE", "storage partially covers expected supply"));
    } else {
      reasons.add(new PressureReason("STORAGE", "storage covers expected supply"));
    }

    PressureBand transportBand = PressureBand.LOW;
    double transportCoverage = snapshot.getTransportAvailableTonnes() / effective;
    if (snapshot.getTransportAvailableTonnes() <= 0) {
      transportBand = PressureBand.HIGH;
      reasons.add(
          new PressureReason(
              "TRANSPORT", "no transport available to move expected supply"));
    } else if (transportCoverage < 0.5) {
      transportBand = PressureBand.HIGH;
      reasons.add(
          new PressureReason(
              "TRANSPORT",
              "transport capacity is constrained (covers %.0f%% of expected supply)"
                  .formatted(transportCoverage * 100)));
    } else if (transportCoverage < 1.0) {
      transportBand = PressureBand.MODERATE;
      reasons.add(new PressureReason("TRANSPORT", "transport partially covers expected supply"));
    } else {
      reasons.add(new PressureReason("TRANSPORT", "transport covers expected supply"));
    }

    double processingCoverage = snapshot.getProcessingAvailableTonnes() / effective;
    if (snapshot.getProcessingAvailableTonnes() <= 0) {
      reasons.add(new PressureReason("PROCESSING", "no processing capacity in the region"));
    } else if (processingCoverage >= 1.0) {
      reasons.add(
          new PressureReason("PROCESSING", "processing capacity covers expected supply"));
    } else {
      reasons.add(
          new PressureReason(
              "PROCESSING",
              "processing covers %.0f%% of expected supply".formatted(processingCoverage * 100)));
    }

    double confirmedShare = demand.getConfirmedDemandTonnes() / estMax;
    reasons.add(
        new PressureReason(
            "DEMAND",
            "confirmed buyer demand covers %.0f%% of estimated demand"
                .formatted(confirmedShare * 100)));

    PressureBand overall = highest(supplyBand, storageBand, transportBand);
    if (demand.getConfirmedDemandTonnes() < 0.3 * estMax && overall.ordinal() >= PressureBand.MODERATE.ordinal()
        && overall != PressureBand.CRITICAL) {
      overall = PressureBand.values()[overall.ordinal() + 1];
      reasons.add(
          new PressureReason(
              "DEMAND", "weak confirmed demand escalates pressure one level"));
    }
    return new PressureAssessment(
        overall, effective, estMin, demand.getEstimatedMaxTonnes(), reasons);
  }

  /** Confidence-weighted active supply. Read-only; reused by the simulator. */
  @Transactional(readOnly = true)
  public double effectiveSupply(Long cropId, String region) {
    return List.of(ReportStatus.SUBMITTED, ReportStatus.VALIDATED).stream()
        .flatMap(status -> reports.findByRegionAndStatus(region, status).stream())
        .filter(r -> r.getCrop() != null && r.getCrop().getId().equals(cropId))
        .mapToDouble(
            r -> {
              double midpoint = (r.getQuantityMinTonnes() + r.getQuantityMaxTonnes()) / 2.0;
              double weight =
                  trustScores
                      .findBySupplyReportId(r.getId())
                      .map(ts -> ts.getScore() / 100.0)
                      .orElse(1.0);
              return midpoint * weight;
            })
        .sum();
  }

  private PressureBand highest(PressureBand... bands) {
    PressureBand top = PressureBand.LOW;
    for (PressureBand b : bands) {
      if (b.ordinal() > top.ordinal()) {
        top = b;
      }
    }
    return top;
  }
}

