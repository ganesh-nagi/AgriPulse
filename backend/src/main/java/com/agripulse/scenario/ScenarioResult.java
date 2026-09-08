package com.agripulse.scenario;

import com.agripulse.pressure.PressureService.PressureAssessment;

/** Baseline vs scenario outcome. Deterministic: same input, same result. */
public class ScenarioResult {

  private final String label;
  private final PressureAssessment baseline;
  private final PressureAssessment scenario;

  public ScenarioResult(String label, PressureAssessment baseline, PressureAssessment scenario) {
    this.label = label;
    this.baseline = baseline;
    this.scenario = scenario;
  }

  public String getLabel() {
    return label;
  }

  public PressureAssessment getBaseline() {
    return baseline;
  }

  public PressureAssessment getScenario() {
    return scenario;
  }
}
