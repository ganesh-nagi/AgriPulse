package com.agripulse.crop;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reference crop list for farmer supply-report selection. */
@RestController
@RequestMapping("/api/crops")
public class CropController {

  private final CropRepository crops;

  public CropController(CropRepository crops) {
    this.crops = crops;
  }

  @GetMapping
  public List<CropSummary> list() {
    return crops.findAll().stream()
        .map(
            c ->
                new CropSummary(c.getId(), c.getName(), c.getUnit()))
        .toList();
  }

  record CropSummary(Long id, String name, String unit) {}
}
