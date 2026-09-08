package com.agripulse.resource;

import com.agripulse.market.MarketRepository;
import com.agripulse.processing.ProcessingFacilityRepository;
import com.agripulse.storage.StorageFacilityRepository;
import com.agripulse.transport.TransportResourceRepository;
import com.agripulse.transport.TransportStatus;
import java.time.LocalDate;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only resource state for later allocation phases.
 *
 * <p>Inclusion rules (all must hold):
 * <ul>
 *   <li>Storage: same region, crop-compatible (blank compatibility accepts
 *       any crop, otherwise comma-separated names, case-insensitive), and
 *       the date falls inside the availability window (null bound = open).</li>
 *   <li>Transport: origin region matches, status AVAILABLE, date inside
 *       the availability window.</li>
 *   <li>Processing: same region, same crop, date inside the window.</li>
 *   <li>Market absorption: summed market ranges for the region, kept
 *       separate from demand estimates.</li>
 * </ul>
 */
@Service
public class ResourceStateService {

  private final StorageFacilityRepository storage;
  private final TransportResourceRepository transport;
  private final ProcessingFacilityRepository processing;
  private final MarketRepository markets;

  public ResourceStateService(
      StorageFacilityRepository storage,
      TransportResourceRepository transport,
      ProcessingFacilityRepository processing,
      MarketRepository markets) {
    this.storage = storage;
    this.transport = transport;
    this.processing = processing;
    this.markets = markets;
  }

  @Transactional(readOnly = true)
  public ResourceSnapshot snapshot(String region, Long cropId, String cropName, LocalDate date) {
    double storageAvailable =
        storage.findByRegion(region).stream()
            .filter(f -> isCompatible(f.getCropCompatibility(), cropName))
            .filter(f -> withinWindow(f.getAvailableFrom(), f.getAvailableTo(), date))
            .mapToDouble(f -> Math.max(0.0, f.getCapacityTonnes() - f.getOccupiedTonnes()))
            .sum();

    double transportAvailable =
        transport.findByOriginRegionAndStatus(region, TransportStatus.AVAILABLE).stream()
            .filter(t -> withinWindow(t.getAvailableFrom(), t.getAvailableTo(), date))
            .mapToDouble(t -> Math.max(0.0, t.getCapacityTonnes()))
            .sum();

    double processingAvailable =
        processing.findByRegion(region).stream()
            .filter(p -> p.getCrop() != null && p.getCrop().getId().equals(cropId))
            .filter(p -> withinWindow(p.getAvailableFrom(), p.getAvailableTo(), date))
            .mapToDouble(p -> Math.max(0.0, p.getCapacityTonnes() - p.getOccupiedTonnes()))
            .sum();

    double absorptionMin =
        markets.findByRegion(region).stream().mapToDouble(m -> m.getAbsorptionMinTonnes()).sum();
    double absorptionMax =
        markets.findByRegion(region).stream().mapToDouble(m -> m.getAbsorptionMaxTonnes()).sum();

    return new ResourceSnapshot(
        region,
        cropName,
        date,
        storageAvailable,
        transportAvailable,
        processingAvailable,
        absorptionMin,
        absorptionMax);
  }

  /** Shared rule, also used by the allocation engine. */
  public static boolean isCompatible(String compatibility, String cropName) {
    if (compatibility == null || compatibility.isBlank()) {
      return true;
    }
    return Arrays.stream(compatibility.split(","))
        .map(String::trim)
        .anyMatch(c -> c.equalsIgnoreCase(cropName));
  }

  /** Shared rule, also used by the allocation engine. */
  public static boolean withinWindow(LocalDate from, LocalDate to, LocalDate date) {
    if (date == null) {
      return true;
    }
    if (from != null && date.isBefore(from)) {
      return false;
    }
    return to == null || !date.isAfter(to);
  }
}
