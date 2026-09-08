package com.agripulse.geo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only nearby-infrastructure lookup for map clients. */
@RestController
@RequestMapping("/api/geo")
@PreAuthorize("hasRole('FARMER')")
public class GeoController {

  private final GeoService geoService;

  public GeoController(GeoService geoService) {
    this.geoService = geoService;
  }

  @GetMapping("/nearby")
  public List<GeoNode> nearby(
      @RequestParam double lat,
      @RequestParam double lon,
      @RequestParam(defaultValue = "100") double radiusKm,
      @RequestParam(required = false) Set<String> kinds,
      @RequestParam(required = false) String crop) {
    Set<GeoKind> parsed = null;
    if (kinds != null && !kinds.isEmpty()) {
      parsed =
          kinds.stream()
              .map(k -> GeoKind.valueOf(k.trim().toUpperCase()))
              .collect(Collectors.toSet());
    }
    return geoService.nearby(lat, lon, radiusKm, parsed, crop);
  }
}
