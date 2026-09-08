package com.agripulse.scenario;

import com.agripulse.crop.CropRepository;
import com.agripulse.demand.DemandEstimateResponse;
import com.agripulse.demand.DemandEstimateResponse.SignalProvenance;
import com.agripulse.demand.DemandEstimationService;
import com.agripulse.pressure.PressureService;
import com.agripulse.pressure.PressureService.PressureAssessment;
import com.agripulse.resource.ResourceSnapshot;
import com.agripulse.resource.ResourceStateService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What-if simulator. Reads production state, applies the scenario to
 * in-memory copies only, and re-runs the shared pressure evaluation.
 * The method is read-only and the scenario objects are never entities,
 * so production records cannot be modified. No randomness: identical
 * input always yields identical output.
 */
@Service
public class ScenarioSimulationService {

  private final PressureService pressure;
  private final DemandEstimationService demandEstimation;
  private final ResourceStateService resourceState;
  private final CropRepository crops;

  public ScenarioSimulationService(
      PressureService pressure,
      DemandEstimationService demandEstimation,
      ResourceStateService resourceState,
      CropRepository crops) {
    this.pressure = pressure;
    this.demandEstimation = demandEstimation;
    this.resourceState = resourceState;
    this.crops = crops;
  }

  @Transactional(readOnly = true)
  public ScenarioResult simulate(Scenario scenario) {
    ScenarioInput in = scenario.getInput();
    validate(in);
    String cropName =
        crops.findById(in.getCropId())
            .orElseThrow(() -> new IllegalArgumentException("Crop not found"))
            .getName();
    String normRegion = in.getRegion() == null ? "" : in.getRegion().trim();

    double effective = pressure.effectiveSupply(in.getCropId(), normRegion);
    DemandEstimateResponse demand =
        demandEstimation.computeUnsaved(
            in.getCropId(), normRegion, in.getWindowStart(), in.getWindowEnd());
    ResourceSnapshot snapshot =
        resourceState.snapshot(normRegion, in.getCropId(), cropName, in.getWindowEnd());
    PressureAssessment baseline = pressure.evaluate(effective, demand, snapshot);

    double scenarioEffective = Math.max(0.0, effective + in.getSupplyDeltaTonnes());
    DemandEstimateResponse scenarioDemand = shiftDemand(demand, in.getDemandDeltaTonnes());
    ResourceSnapshot scenarioSnapshot =
        new ResourceSnapshot(
            snapshot.getRegion(),
            snapshot.getCropName(),
            snapshot.getDate(),
            in.getStorageOverrideTonnes() != null
                ? in.getStorageOverrideTonnes()
                : snapshot.getStorageAvailableTonnes(),
            in.getTransportOverrideTonnes() != null
                ? in.getTransportOverrideTonnes()
                : snapshot.getTransportAvailableTonnes(),
            in.getProcessingOverrideTonnes() != null
                ? in.getProcessingOverrideTonnes()
                : snapshot.getProcessingAvailableTonnes(),
            snapshot.getMarketAbsorptionMinTonnes(),
            snapshot.getMarketAbsorptionMaxTonnes());
    PressureAssessment outcome = pressure.evaluate(scenarioEffective, scenarioDemand, scenarioSnapshot);
    return new ScenarioResult(scenario.getLabel(), baseline, outcome);
  }

  private void validate(ScenarioInput in) {
    if (in.getCropId() == null) {
      throw new IllegalArgumentException("cropId is required");
    }
    if (in.getWindowStart() == null
        || in.getWindowEnd() == null
        || in.getWindowStart().isAfter(in.getWindowEnd())) {
      throw new IllegalArgumentException("Valid windowStart/windowEnd required");
    }
    if (negative(in.getStorageOverrideTonnes())
        || negative(in.getTransportOverrideTonnes())
        || negative(in.getProcessingOverrideTonnes())) {
      throw new IllegalArgumentException("Resource overrides cannot be negative");
    }
    if (!Double.isFinite(in.getSupplyDeltaTonnes()) || !Double.isFinite(in.getDemandDeltaTonnes())) {
      throw new IllegalArgumentException("Deltas must be finite numbers");
    }
  }

  private boolean negative(Double value) {
    return value != null && value < 0;
  }

  private DemandEstimateResponse shiftDemand(DemandEstimateResponse demand, double delta) {
    if (delta == 0) {
      return demand;
    }
    List<SignalProvenance> provenance = new ArrayList<>(demand.getProvenance());
    provenance.add(
        new SignalProvenance(
            "SCENARIO",
            demand.getDataSource() == null ? java.util.Set.of() : java.util.Set.of(demand.getDataSource()),
            "demand range shifted by " + delta + " t"));
    return new DemandEstimateResponse(
        demand.getCropName(),
        demand.getRegion(),
        demand.getWindowStart(),
        demand.getWindowEnd(),
        demand.getConfirmedDemandTonnes(),
        demand.getConfirmedBuyerCount(),
        demand.getAbsorptionMinTonnes(),
        demand.getAbsorptionMaxTonnes(),
        demand.getObservationCount(),
        Math.max(0.0, demand.getEstimatedMinTonnes() + delta),
        Math.max(0.0, demand.getEstimatedMaxTonnes() + delta),
        demand.getConfidence(),
        demand.getDataSource(),
        provenance);
  }
}
