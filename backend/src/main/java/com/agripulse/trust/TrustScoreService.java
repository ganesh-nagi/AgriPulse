package com.agripulse.trust;

import com.agripulse.user.VerificationStatus;
import org.springframework.stereotype.Service;

/**
 * Deterministic trust scoring (no AI). Transparent weighted signals, capped
 * below 100 because reports carry uncertainty by definition.
 */
@Service
public class TrustScoreService {

  /** Result of scoring: 0-95 plus a human-readable signal summary. */
  public record Score(double value, String signals) {}

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
    double capped = Math.min(95.0, score);
    return new Score(capped, signals.toString());
  }
}
