package com.agripulse.pressure;

import org.springframework.stereotype.Service;

/**
 * Deterministic market pressure: how estimated peak supply compares to
 * estimated peak absorption. Pure function, fully testable, no AI.
 */
@Service
public class PressureService {

  /** Pressure outcome with the ratio that produced it and an explanation. */
  public record PressureResult(double ratio, PressureBand band, String explanation) {}

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
}
