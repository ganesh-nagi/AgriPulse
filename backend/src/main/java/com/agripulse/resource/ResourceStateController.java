package com.agripulse.resource;

import com.agripulse.crop.CropRepository;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only resource snapshot for farmer clients. Totals only; no
 * operator identities or exact facility positions.
 */
@RestController
@RequestMapping("/api/resources")
@PreAuthorize("hasRole('FARMER')")
public class ResourceStateController {

  private final ResourceStateService resourceStateService;
  private final CropRepository crops;

  public ResourceStateController(ResourceStateService resourceStateService, CropRepository crops) {
    this.resourceStateService = resourceStateService;
    this.crops = crops;
  }

  @GetMapping("/snapshot")
  public ResourceSnapshot snapshot(
      @RequestParam String region,
      @RequestParam Long cropId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    String cropName =
        crops.findById(cropId).orElseThrow(() -> new IllegalArgumentException("Crop not found"))
            .getName();
    return resourceStateService.snapshot(region, cropId, cropName, date);
  }
}
