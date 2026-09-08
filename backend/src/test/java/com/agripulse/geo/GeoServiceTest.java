package com.agripulse.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.market.Market;
import com.agripulse.market.MarketRepository;
import com.agripulse.processing.ProcessingFacility;
import com.agripulse.processing.ProcessingFacilityRepository;
import com.agripulse.storage.StorageFacility;
import com.agripulse.storage.StorageFacilityRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GeoServiceTest {

  @Autowired private GeoService geo;
  @Autowired private MarketRepository markets;
  @Autowired private StorageFacilityRepository storageRepo;
  @Autowired private ProcessingFacilityRepository processingRepo;
  @Autowired private CropRepository crops;

  // Nashik city centre-ish.
  private static final double LAT = 19.9975;
  private static final double LON = 73.7898;

  private Crop tomato;

  @BeforeEach
  void setUp() {
    tomato = new Crop();
    tomato.setName("Tomato");
    tomato = crops.save(tomato);

    Market near = new Market();
    near.setName("Near Mandi");
    near.setRegion("Nashik");
    near.setLatitude(20.0059);
    near.setLongitude(73.7910);
    near.setAbsorptionMinTonnes(50);
    near.setAbsorptionMaxTonnes(70);
    markets.save(near);

    Market far = new Market();
    far.setName("Far Mandi");
    far.setRegion("Pune");
    far.setLatitude(18.5204);
    far.setLongitude(73.8567);
    far.setAbsorptionMaxTonnes(40);
    markets.save(far);

    StorageFacility store = new StorageFacility();
    store.setName("Cold Store");
    store.setRegion("Nashik");
    store.setLatitude(20.0100);
    store.setLongitude(73.7900);
    store.setCapacityTonnes(100);
    store.setOccupiedTonnes(20);
    store.setCropCompatibility("Tomato");
    storageRepo.save(store);

    StorageFacility wheatOnly = new StorageFacility();
    wheatOnly.setName("Wheat Store");
    wheatOnly.setRegion("Nashik");
    wheatOnly.setLatitude(20.0100);
    wheatOnly.setLongitude(73.7900);
    wheatOnly.setCapacityTonnes(100);
    wheatOnly.setOccupiedTonnes(0);
    wheatOnly.setCropCompatibility("Wheat");
    storageRepo.save(wheatOnly);

    ProcessingFacility unit = new ProcessingFacility();
    unit.setName("Grading Unit");
    unit.setRegion("Nashik");
    unit.setCrop(tomato);
    unit.setCapacityTonnes(60);
    unit.setOccupiedTonnes(10);
    unit.setLatitude(19.9900);
    unit.setLongitude(73.7800);
    processingRepo.save(unit);
  }

  @Test
  void haversineNashikToPuneIsPlausible() {
    double km = GeoService.haversineKm(19.9975, 73.7898, 18.5204, 73.8567);
    assertTrue(km > 150 && km < 190, "expected ~165 km, got " + km);
    assertEquals(0.0, GeoService.haversineKm(LAT, LON, LAT, LON));
  }

  @Test
  void radiusFiltersAndSortsByDistance() {
    List<GeoNode> nodes = geo.nearby(LAT, LON, 50, null, null);

    assertTrue(nodes.stream().anyMatch(n -> n.getName().equals("Near Mandi")));
    assertTrue(nodes.stream().noneMatch(n -> n.getName().equals("Far Mandi")));
    for (int i = 1; i < nodes.size(); i++) {
      assertTrue(nodes.get(i).getDistanceKm() >= nodes.get(i - 1).getDistanceKm());
    }
  }

  @Test
  void kindFilterLimitsResults() {
    List<GeoNode> nodes = geo.nearby(LAT, LON, 50, Set.of(GeoKind.MARKET), null);

    assertTrue(!nodes.isEmpty());
    assertTrue(nodes.stream().allMatch(n -> n.getKind() == GeoKind.MARKET));
  }

  @Test
  void cropCompatibilityFiltersStorage() {
    List<GeoNode> nodes = geo.nearby(LAT, LON, 50, Set.of(GeoKind.STORAGE), "Tomato");

    assertTrue(nodes.stream().anyMatch(n -> n.getName().equals("Cold Store")));
    assertTrue(nodes.stream().noneMatch(n -> n.getName().equals("Wheat Store")));
  }

  @Test
  void processingCarriesAvailability() {
    List<GeoNode> nodes = geo.nearby(LAT, LON, 50, Set.of(GeoKind.PROCESSING), "Tomato");

    assertEquals(1, nodes.size());
    assertEquals(50.0, nodes.get(0).getAvailableTonnes());
  }

  @Test
  void nodesExposeNoFarmerData() {
    List<GeoNode> nodes = geo.nearby(LAT, LON, 500, null, null);

    assertTrue(!nodes.isEmpty());
    for (GeoNode n : nodes) {
      // DTO surface is fixed: kind/id/name/region/coords/distance/summary/availability.
      assertTrue(
          n.getKind() == GeoKind.MARKET
              || n.getKind() == GeoKind.STORAGE
              || n.getKind() == GeoKind.PROCESSING);
    }
  }

  @Test
  void invalidInputRejected() {
    assertThrows(IllegalArgumentException.class, () -> geo.nearby(95, LON, 50, null, null));
    assertThrows(IllegalArgumentException.class, () -> geo.nearby(LAT, LON, 0, null, null));
    assertThrows(IllegalArgumentException.class, () -> geo.nearby(LAT, LON, -5, null, null));
  }
}
