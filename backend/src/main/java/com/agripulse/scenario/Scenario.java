package com.agripulse.scenario;

/**
 * A what-if scenario: validated input bundle plus a human label.
 * Deliberately not a JPA entity: scenarios are never persisted and can
 * never alter production records.
 */
public class Scenario {

  private final String label;
  private final ScenarioInput input;

  public Scenario(String label, ScenarioInput input) {
    this.label = label;
    this.input = input;
  }

  public String getLabel() {
    return label;
  }

  public ScenarioInput getInput() {
    return input;
  }
}
