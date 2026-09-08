package com.agripulse.allocation;

import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.demand.BuyerRequirement;
import com.agripulse.demand.BuyerRequirementRepository;
import com.agripulse.demand.RequirementStatus;
import com.agripulse.market.Market;
import com.agripulse.market.MarketRepository;
import com.agripulse.processing.ProcessingFacilityRepository;
import com.agripulse.resource.ResourceStateService;
import com.agripulse.storage.StorageFacility;
import com.agripulse.storage.StorageFacilityRepository;
import com.agripulse.transport.TransportResource;
import com.agripulse.transport.TransportResourceRepository;
import com.agripulse.transport.TransportStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Surplus placement engine. Deterministic greedy strategy (NOT an AI
 * optimizer): fill compatible storage, then processing, then confirmed
 * buyer demand, then alternative markets limited by shared transport
 * capacity. The strategy lives in {@link #placeSurplus} so a mathematical
 * optimizer (e.g. OR-Tools) can replace it later behind this same
 * interface; OR-Tools was deliberately not added because its native
 * binaries and extra dependency graph threaten MVP stability for a
 * problem this greedy pass solves transparently.
 *
 * <p>Rules: capacity, crop compatibility, availability windows and
 * transport limits are hard constraints. Same-region sinks are assumed to
 * need no transport; every alternative-market leg consumes the shared
 * transport pool. Cost estimates use known per-day/per-km rates only and
 * are labelled estimates; null means no rate data. Read-only: nothing is
 * booked or mutated.
 */
@Service
public class AllocationService {

  private final StorageFacilityRepository storage;
  private final ProcessingFacilityRepository processing;
  private final BuyerRequirementRepository requirements;
  private final MarketRepository markets;
  private final TransportResourceRepository transport;
  private final CropRepository crops;

  public AllocationService(
      StorageFacilityRepository storage,
      ProcessingFacilityRepository processing,
      BuyerRequirementRepository requirements,
      MarketRepository markets,
      TransportResourceRepository transport,
      CropRepository crops) {
    this.storage = storage;
    this.processing = processing;
    this.requirements = requirements;
    this.markets = markets;
    this.transport = transport;
    this.crops = crops;
  }

  @Transactional(readOnly = true)
  public AllocationResult allocate(AllocationRequest request) {
    if (request.getCropId() == null) {
      throw new IllegalArgumentException("cropId is required");
    }
    if (request.getWindowStart() == null
        || request.getWindowEnd() == null
        || request.getWindowStart().isAfter(request.getWindowEnd())) {
      throw new IllegalArgumentException("Valid windowStart/windowEnd required");
    }
    if (request.getSurplusTonnes() < 0 || !Double.isFinite(request.getSurplusTonnes())) {
      throw new IllegalArgumentException("surplusTonnes must be a finite value >= 0");
    }
    Crop crop =
        crops
            .findById(request.getCropId())
            .orElseThrow(() -> new IllegalArgumentException("Crop not found"));
    String region = request.getRegion() == null ? "" : request.getRegion().trim();
    return placeSurplus(crop, region, request.getWindowStart(), request.getWindowEnd(),
        request.getSurplusTonnes());
  }

  private AllocationResult placeSurplus(
      Crop crop, String region, LocalDate windowStart, LocalDate windowEnd, double surplus) {
    List<AllocationEntry> entries = new ArrayList<>();
    double remaining = surplus;
    LocalDate asOf = windowEnd;

    List<StorageFacility> stores =
        storage.findByRegion(region).stream()
            .filter(s -> ResourceStateService.isCompatible(s.getCropCompatibility(), crop.getName()))
            .filter(s -> ResourceStateService.withinWindow(s.getAvailableFrom(), s.getAvailableTo(), asOf))
            .filter(s -> s.getCapacityTonnes() - s.getOccupiedTonnes() > 0)
            .sorted(
                Comparator.comparing(
                        (StorageFacility s) -> s.getCostPerDay() == null ? Double.MAX_VALUE
                            : s.getCostPerDay())
                    .thenComparing(
                        (StorageFacility s) -> s.getCapacityTonnes() - s.getOccupiedTonnes(),
                        Comparator.reverseOrder())
                    .thenComparing(StorageFacility::getId))
            .toList();
    for (StorageFacility s : stores) {
      if (remaining <= 0) break;
      double available = s.getCapacityTonnes() - s.getOccupiedTonnes();
      double placed = Math.min(remaining, available);
      Double cost = s.getCostPerDay() == null ? null : s.getCostPerDay() * placed;
      entries.add(
          new AllocationEntry(
              "STORAGE",
              s.getId(),
              s.getName(),
              placed,
              "compatible storage within availability window"
                  + (cost != null ? "; cost is a 1-day holding estimate" : ""),
              cost));
      remaining -= placed;
    }

    if (remaining > 0) {
      var units =
          processing.findByRegion(region).stream()
              .filter(p -> p.getCrop() != null && p.getCrop().getId().equals(crop.getId()))
              .filter(p -> ResourceStateService.withinWindow(p.getAvailableFrom(), p.getAvailableTo(), asOf))
              .filter(p -> p.getCapacityTonnes() - p.getOccupiedTonnes() > 0)
              .sorted(Comparator.comparing(
                  (com.agripulse.processing.ProcessingFacility p) ->
                      p.getCapacityTonnes() - p.getOccupiedTonnes(),
                  Comparator.reverseOrder())
                  .thenComparing(com.agripulse.processing.ProcessingFacility::getId))
              .toList();
      for (var p : units) {
        if (remaining <= 0) break;
        double placed = Math.min(remaining, p.getCapacityTonnes() - p.getOccupiedTonnes());
        Double cost = p.getCostPerDay() == null ? null : p.getCostPerDay() * placed;
        entries.add(
            new AllocationEntry(
                "PROCESSING",
                p.getId(),
                p.getName(),
                placed,
                "crop-matched processing within availability window"
                    + (cost != null ? "; cost is a 1-day estimate" : ""),
                cost));
        remaining -= placed;
      }
    }

    if (remaining > 0) {
      List<BuyerRequirement> open =
          requirements.findByCropIdAndRegionAndStatusInAndRequiredDateBetween(
              crop.getId(), region, List.of(RequirementStatus.OPEN), windowStart, windowEnd);
      double demandCapacity = open.stream().mapToDouble(BuyerRequirement::getQuantityTonnes).sum();
      if (demandCapacity > 0) {
        double placed = Math.min(remaining, demandCapacity);
        entries.add(
            new AllocationEntry(
                "BUYER_DEMAND",
                null,
                "Confirmed buyer demand (" + open.size() + " open requirements)",
                placed,
                "direct sale against confirmed requirements; no movement assumed",
                null));
        remaining -= placed;
      }
    }

    if (remaining > 0) {
      List<TransportResource> fleet =
          transport.findByOriginRegionAndStatus(region, TransportStatus.AVAILABLE).stream()
              .filter(t -> ResourceStateService.withinWindow(t.getAvailableFrom(), t.getAvailableTo(), asOf))
              .filter(t -> t.getCapacityTonnes() > 0)
              .toList();
      double transportLeft = fleet.stream().mapToDouble(TransportResource::getCapacityTonnes).sum();
      Double cheapestRate =
          fleet.stream()
              .map(TransportResource::getCostPerKm)
              .filter(r -> r != null)
              .min(Double::compareTo)
              .orElse(null);
      List<Market> alternatives =
          markets.findAll().stream()
              .filter(m -> !region.equalsIgnoreCase(m.getRegion()))
              .filter(m -> m.getAbsorptionMaxTonnes() > 0)
              .sorted(Comparator.comparing(Market::getId))
              .toList();
      for (Market m : alternatives) {
        if (remaining <= 0 || transportLeft <= 0) break;
        double placed = Math.min(remaining, Math.min(m.getAbsorptionMaxTonnes(), transportLeft));
        if (placed <= 0) continue;
        Double cost = cheapestRate == null ? null : cheapestRate * placed;
        entries.add(
            new AllocationEntry(
                "ALTERNATIVE_MARKET",
                m.getId(),
                m.getName(),
                placed,
                "spare absorption at alternative market; consumes shared transport pool"
                    + (cost != null ? "; cost is a per-km rate estimate, distance unknown" : ""),
                cost));
        remaining -= placed;
        transportLeft -= placed;
      }
    }

    double allocated = surplus - Math.max(0.0, remaining);
    double exposure = Math.max(0.0, remaining);
    return new AllocationResult(surplus, entries, allocated, exposure, exposure <= 0);
  }
}
