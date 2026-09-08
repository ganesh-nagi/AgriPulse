package com.agripulse.pressure;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only pressure views for farmer clients. All logic lives in
 * {@link PressureService}; regional aggregates only, no identities.
 */
@RestController
@RequestMapping("/api/pressure")
@PreAuthorize("hasRole('FARMER')")
public class PressureController {

  private final PressureService pressureService;

  public PressureController(PressureService pressureService) {
    this.pressureService = pressureService;
  }

  @GetMapping
  public PressureService.PressureAssessment assess(
      @RequestParam Long cropId,
      @RequestParam String region,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return pressureService.assess(cropId, region, from, to);
  }
}
