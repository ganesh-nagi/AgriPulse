package com.agripulse.supply;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public regional aggregates. Safe by construction: the DTO carries no
 * identities, no coordinates and no individual quantities.
 */
@RestController
@RequestMapping("/api/public/regional")
public class RegionalSupplyController {

  private final RegionalSupplyService regionalSupplyService;

  public RegionalSupplyController(RegionalSupplyService regionalSupplyService) {
    this.regionalSupplyService = regionalSupplyService;
  }

  @GetMapping("/supply")
  public List<RegionalSupplyView> supply(@RequestParam String region) {
    return regionalSupplyService.aggregateByRegion(region.trim());
  }
}
