package com.agripulse.geo;

import com.agripulse.market.MarketRepository;
import com.agripulse.processing.ProcessingFacilityRepository;
import com.agripulse.resource.ResourceStateService;
import com.agripulse.storage.StorageFacilityRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nearby public infrastructure over plain lat/long columns using the
 * Haversine formula (MVP-acceptable where routing is unnecessary; swap
 * for PostGIS geography functions later without changing this interface).
 * Only zone/market-level nodes are ever returned — farmer locations and
 * identities cannot appear here by construction.
 */
@Service
public class GeoService {

  static final double EARTH_RADIUS_KM = 6371.0;

  private final MarketRepository markets;
  private final StorageFacilityRepository storage;
  private final ProcessingFacilityRepository processing;

  public GeoService(
      MarketRepository markets,
      StorageFacilityRepository storage,
      ProcessingFacilityRepository processing) {
    this.markets = markets;
    this.storage = storage;
    this.processing = processing;
  }

  /** Great-circle distance in kilometres. Pure function. */
  public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
    return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
  }

  @Transactional(readOnly = true)
  public List<GeoNode> nearby(
      double latitude, double longitude, double radiusKm, Set<GeoKind> kinds, String cropName) {
    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
      throw new IllegalArgumentException("Latitude/longitude out of range");
    }
    if (radiusKm <= 0 || radiusKm > 2000 || !Double.isFinite(radiusKm)) {
      throw new IllegalArgumentException("radiusKm must be within (0, 2000]");
    }
    Set<GeoKind> wanted =
        kinds == null || kinds.isEmpty() ? EnumSet.allOf(GeoKind.class) : EnumSet.copyOf(kinds);
    List<GeoNode> nodes = new ArrayList<>();

    if (wanted.contains(GeoKind.MARKET)) {
      markets.findAll().stream()
          .filter(m -> m.getLatitude() != null && m.getLongitude() != null)
          .map(
              m ->
                  new GeoNode(
                      GeoKind.MARKET,
                      m.getId(),
                      m.getName(),
                      m.getRegion(),
                      m.getLatitude(),
                      m.getLongitude(),
                      haversineKm(latitude, longitude, m.getLatitude(), m.getLongitude()),
                      "Absorption %.0f-%.0f t".formatted(
                          m.getAbsorptionMinTonnes(), m.getAbsorptionMaxTonnes()),
                      m.getAbsorptionMaxTonnes()))
          .filter(n -> n.getDistanceKm() <= radiusKm)
          .forEach(nodes::add);
    }
    if (wanted.contains(GeoKind.STORAGE)) {
      storage.findAll().stream()
          .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
          .filter(
              s ->
                  cropName == null
                      || cropName.isBlank()
                      || ResourceStateService.isCompatible(s.getCropCompatibility(), cropName))
          .map(
              s -> {
                double available =
                    Math.max(0.0, s.getCapacityTonnes() - s.getOccupiedTonnes());
                return new GeoNode(
                    GeoKind.STORAGE,
                    s.getId(),
                    s.getName(),
                    s.getRegion(),
                    s.getLatitude(),
                    s.getLongitude(),
                    haversineKm(latitude, longitude, s.getLatitude(), s.getLongitude()),
                    "Storage available %.0f t".formatted(available),
                    available);
              })
          .filter(n -> n.getDistanceKm() <= radiusKm)
          .forEach(nodes::add);
    }
    if (wanted.contains(GeoKind.PROCESSING)) {
      processing.findAll().stream()
          .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
          .filter(
              p ->
                  cropName == null
                      || cropName.isBlank()
                      || (p.getCrop() != null && p.getCrop().getName().equalsIgnoreCase(cropName)))
          .map(
              p -> {
                double available =
                    Math.max(0.0, p.getCapacityTonnes() - p.getOccupiedTonnes());
                String crop = p.getCrop() != null ? p.getCrop().getName() : "?";
                return new GeoNode(
                    GeoKind.PROCESSING,
                    p.getId(),
                    p.getName(),
                    p.getRegion(),
                    p.getLatitude(),
                    p.getLongitude(),
                    haversineKm(latitude, longitude, p.getLatitude(), p.getLongitude()),
                    "Processing %s, available %.0f t".formatted(crop, available),
                    available);
              })
          .filter(n -> n.getDistanceKm() <= radiusKm)
          .forEach(nodes::add);
    }
    nodes.sort(
        Comparator.comparingDouble(GeoNode::getDistanceKm).thenComparing(GeoNode::getId));
    return nodes;
  }
}
