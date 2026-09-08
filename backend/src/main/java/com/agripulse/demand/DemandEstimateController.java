package com.agripulse.demand;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only demand picture for farmer clients. Aggregates with provenance
 * only; no buyer identities are exposed.
 */
@RestController
@RequestMapping("/api/demand")
@PreAuthorize("hasRole('FARMER')")
public class DemandEstimateController {

  private final DemandEstimationService demandEstimationService;

  public DemandEstimateController(DemandEstimationService demandEstimationService) {
    this.demandEstimationService = demandEstimationService;
  }

  @GetMapping("/estimate")
  public DemandEstimateResponse estimate(
      @RequestParam Long cropId,
      @RequestParam String region,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return demandEstimationService.estimate(cropId, region, from, to);
  }
}
