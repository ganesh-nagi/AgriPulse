package com.agripulse.trust;

/**
 * Reliability band of a single supply report.
 *
 * <p>Low-confidence reports are down-weighted in regional aggregation, never
 * deleted. {@code REQUIRES_REVIEW} routes a report to human/FPO review; the
 * engine never labels a report as fraud.
 */
public enum TrustLevel {
  HIGH_CONFIDENCE,
  MEDIUM_CONFIDENCE,
  LOW_CONFIDENCE,
  REQUIRES_REVIEW
}
