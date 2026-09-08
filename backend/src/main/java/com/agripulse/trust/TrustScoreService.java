package com.agripulse.trust;

import com.agripulse.user.VerificationStatus;
import org.springframework.stereotype.Service;

/**
 * Deterministic supply-report trust engine (no AI, no LLM).
 *
 * <p>Principle: a report is never claimed to be "100% true". The engine
 * produces a confidence value (0-95, capped below 100 because every report
 * carries uncertainty), a reliability band, and a review flag. Confidence is
 * a weight for later regional supply aggregation: no single raw report may
 * directly determine regional supply.
 *
 * <h2>Weighted scoring model (all weights fixed and documented)</h2>
 * <pre>
 * base                                                             +20
 * phone-level verification (PHONE/IDENTITY/FPO status)             +10
 * identity-level verification (IDENTITY/FPO status)                +20
 * FPO validation (flag or FPO_VALIDATED status)                    +25
 * supporting evidence attached                                     +15
 * farm region consistent with farmer profile                       +10
 * historical reliability: past average confidence &gt;= 70             +10
 * historical reliability: past average confidence 50-70             +5
 * new reporter (no past reports, limited history)                   -5
 * cross-source consistency (aligned with peer reports)              +5
 * unusual quantity (deviation ratio &gt; 2x)                         -8
 * unusual quantity (deviation ratio &gt; 3x)                        -15
 * temporal consistency (sane harvest window)                        +5
 * temporal inconsistency                                            -10
 * </pre>
 *
 * <p>Missing evidence never rejects a report on its own. Suspicious reports
 * land in LOW_CONFIDENCE or REQUIRES_REVIEW; the engine has no fraud label.
 * Result is clamped to [0, 95] and is a pure function of its input, so
 * repeated calculation always yields the same assessment.
 */
@Service
public class TrustScoreService {

  /** Result of scoring: 0-95 plus a human-readable signal summary. */
  public record Score(double value, String signals) {}

  /**
   * Pure input bundle for {@link #assess(TrustSignals)}. Callers (supply
   * service now, aggregation later) resolve history/peer signals from
   * repositories; the engine itself stays side-effect free.
   *
   * @param verificationStatus account verification tier of the reporter
   * @param hasEvidence whether supporting evidence is attached
   * @param fpoValidated whether the farmer is FPO-validated
   * @param regionConsistent whether farm region matches the farmer profile
   * @param pastReportCount number of earlier reports by this reporter
   * @param pastAverageConfidence average confidence of earlier reports (0-100)
   * @param quantityDeviationRatio reported midpoint divided by the peer
   *     median for the same crop and region; 1.0 means aligned. Values
   *     &lt;= 0 mean "unknown" and are treated as neutral (no bonus/penalty).
   * @param temporalConsistent whether the harvest window is sane
   *     (ordered dates, plausible length and horizon)
   * @param crossSourceConsistent whether quality/region details align with
   *     peer reports
   */
  public record TrustSignals(
      VerificationStatus verificationStatus,
      boolean hasEvidence,
      boolean fpoValidated,
      boolean regionConsistent,
      int pastReportCount,
      double pastAverageConfidence,
      double quantityDeviationRatio,
      boolean temporalConsistent,
      boolean crossSourceConsistent) {}

  /** Full assessment: confidence weight, reliability band, review flag. */
  public record TrustAssessment(
      double confidence, TrustLevel level, boolean requiresReview, String signals) {}

  static final double MAX_CONFIDENCE = 95.0;
  static final double HIGH_THRESHOLD = 70.0;
  static final double MEDIUM_THRESHOLD = 45.0;
  static final double LOW_THRESHOLD = 25.0;

  public Score computeInitialScore(
      VerificationStatus status, boolean hasEvidence, boolean fpoValidated) {
    return computeScore(status, hasEvidence, fpoValidated, false);
  }

  public Score computeScore(
      VerificationStatus status,
      boolean hasEvidence,
      boolean fpoValidated,
      boolean regionConsistent) {
    double score = 20.0;
    StringBuilder signals = new StringBuilder("base=20");
    if (status == VerificationStatus.PHONE_VERIFIED
        || status == VerificationStatus.IDENTITY_VERIFIED
        || status == VerificationStatus.FPO_VALIDATED) {
      score += 10.0;
      signals.append(";phone_verified=+10");
    }
    if (status == VerificationStatus.IDENTITY_VERIFIED
        || status == VerificationStatus.FPO_VALIDATED) {
      score += 20.0;
      signals.append(";identity_verified=+20");
    }
    if (fpoValidated || status == VerificationStatus.FPO_VALIDATED) {
      score += 25.0;
      signals.append(";fpo_validated=+25");
    }
    if (hasEvidence) {
      score += 15.0;
      signals.append(";evidence=+15");
    }
    if (regionConsistent) {
      score += 10.0;
      signals.append(";region_consistent=+10");
    }
    double capped = Math.min(MAX_CONFIDENCE, score);
    return new Score(capped, signals.toString());
  }

  /**
   * Full deterministic assessment over all supported signals. Pure function:
   * same input always yields the same assessment.
   */
  public TrustAssessment assess(TrustSignals in) {
    Score base =
        computeScore(
            in.verificationStatus(), in.hasEvidence(), in.fpoValidated(), in.regionConsistent());
    double confidence = base.value();
    StringBuilder signals = new StringBuilder(base.signals());

    if (in.pastReportCount() <= 0) {
      confidence -= 5.0;
      signals.append(";new_reporter=-5");
    } else if (in.pastAverageConfidence() >= 70.0) {
      confidence += 10.0;
      signals.append(";history_strong=+10");
    } else if (in.pastAverageConfidence() >= 50.0) {
      confidence += 5.0;
      signals.append(";history_moderate=+5");
    } else {
      signals.append(";history_weak=+0");
    }

    boolean deviationKnown = in.quantityDeviationRatio() > 0;
    boolean extremeDeviation =
        deviationKnown
            && (in.quantityDeviationRatio() >= 3.0 || in.quantityDeviationRatio() <= 1.0 / 3.0);
    if (extremeDeviation) {
      confidence -= 15.0;
      signals.append(";quantity_deviation_extreme=-15");
    } else if (deviationKnown && (in.quantityDeviationRatio() >= 2.0 || in.quantityDeviationRatio() <= 0.5)) {
      confidence -= 8.0;
      signals.append(";quantity_deviation_high=-8");
    } else if (in.crossSourceConsistent()) {
      confidence += 5.0;
      signals.append(";cross_source_consistent=+5");
    } else {
      signals.append(";cross_source_unknown=+0");
    }

    if (in.temporalConsistent()) {
      confidence += 5.0;
      signals.append(";temporal_consistent=+5");
    } else {
      confidence -= 10.0;
      signals.append(";temporal_inconsistent=-10");
    }

    confidence = Math.max(0.0, Math.min(MAX_CONFIDENCE, confidence));

    TrustLevel level;
    if (confidence >= HIGH_THRESHOLD) {
      level = TrustLevel.HIGH_CONFIDENCE;
    } else if (confidence >= MEDIUM_THRESHOLD) {
      level = TrustLevel.MEDIUM_CONFIDENCE;
    } else if (confidence >= LOW_THRESHOLD) {
      level = TrustLevel.LOW_CONFIDENCE;
    } else {
      level = TrustLevel.REQUIRES_REVIEW;
    }
    signals.append(";level=").append(level.name());

    boolean requiresReview =
        level == TrustLevel.REQUIRES_REVIEW || extremeDeviation || !in.temporalConsistent();
    return new TrustAssessment(confidence, level, requiresReview, signals.toString());
  }
}
