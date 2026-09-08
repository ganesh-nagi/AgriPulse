package com.agripulse.processing;

import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processing capacity state. Same invariants as storage: capacity &gt;= 0,
 * 0 &lt;= occupied &lt;= capacity, ordered availability window. Zero capacity
 * is valid (no processing available).
 */
@Service
public class ProcessingService {

  private final ProcessingFacilityRepository facilities;
  private final CropRepository crops;

  public ProcessingService(ProcessingFacilityRepository facilities, CropRepository crops) {
    this.facilities = facilities;
    this.crops = crops;
  }

  @Transactional
  public ProcessingFacility register(
      String name,
      String region,
      Long cropId,
      double capacityTonnes,
      double occupiedTonnes,
      LocalDate availableFrom,
      LocalDate availableTo) {
    if (capacityTonnes < 0) {
      throw new IllegalArgumentException("Processing capacity cannot be negative");
    }
    if (occupiedTonnes < 0 || occupiedTonnes > capacityTonnes) {
      throw new IllegalArgumentException("Occupied capacity must be within [0, total capacity]");
    }
    if (availableFrom != null && availableTo != null && availableFrom.isAfter(availableTo)) {
      throw new IllegalArgumentException("availableFrom must be on or before availableTo");
    }
    Crop crop =
        crops.findById(cropId).orElseThrow(() -> new IllegalArgumentException("Crop not found"));
    ProcessingFacility facility = new ProcessingFacility();
    facility.setName(name.trim());
    facility.setRegion(region.trim());
    facility.setCrop(crop);
    facility.setCapacityTonnes(capacityTonnes);
    facility.setOccupiedTonnes(occupiedTonnes);
    facility.setAvailableFrom(availableFrom);
    facility.setAvailableTo(availableTo);
    return facilities.save(facility);
  }
}
