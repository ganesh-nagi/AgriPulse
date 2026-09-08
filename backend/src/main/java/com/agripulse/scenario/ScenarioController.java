package com.agripulse.scenario;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only what-if execution. Delegates to {@link ScenarioSimulationService};
 * no production state is modified.
 */
@RestController
@RequestMapping("/api/scenarios")
@PreAuthorize("hasRole('ADMIN')")
public class ScenarioController {

  private final ScenarioSimulationService simulationService;

  public ScenarioController(ScenarioSimulationService simulationService) {
    this.simulationService = simulationService;
  }

  @PostMapping("/simulate")
  public ScenarioResult simulate(@RequestBody SimulateRequest request) {
    String label = request.getLabel() == null ? "what-if" : request.getLabel();
    return simulationService.simulate(new Scenario(label, request.getInput()));
  }

  public static class SimulateRequest {
    private String label;
    private ScenarioInput input;

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public ScenarioInput getInput() {
      return input;
    }

    public void setInput(ScenarioInput input) {
      this.input = input;
    }
  }
}
