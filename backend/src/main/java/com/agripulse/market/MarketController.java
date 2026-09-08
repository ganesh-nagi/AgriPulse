package com.agripulse.market;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only public market nodes for farmer clients. */
@RestController
@RequestMapping("/api/markets")
@PreAuthorize("hasRole('FARMER')")
public class MarketController {

  private final MarketRepository markets;

  public MarketController(MarketRepository markets) {
    this.markets = markets;
  }

  @GetMapping
  public List<MarketNodeView> list(@RequestParam String region) {
    return markets.findByRegion(region.trim()).stream()
        .map(
            m ->
                new MarketNodeView(
                    m.getId(),
                    m.getName(),
                    m.getRegion(),
                    m.getAbsorptionMinTonnes(),
                    m.getAbsorptionMaxTonnes(),
                    m.getMarketType(),
                    m.getLatitude(),
                    m.getLongitude()))
        .toList();
  }
}
