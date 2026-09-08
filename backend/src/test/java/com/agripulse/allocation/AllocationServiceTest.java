package com.agripulse.allocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agripulse.buyer.BuyerProfile;
import com.agripulse.buyer.BuyerProfileRepository;
import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.demand.BuyerRequirement;
import com.agripulse.demand.BuyerRequirementRepository;
import com.agripulse.demand.DataSource;
import com.agripulse.demand.RequirementStatus;
import com.agripulse.market.Market;
import com.agripulse.market.MarketRepository;
import com.agripulse.processing.ProcessingFacility;
import com.agripulse.processing.ProcessingFacilityRepository;
import com.agripulse.storage.StorageFacility;
import com.agripulse.storage.StorageFacilityRepository;
import com.agripulse.transport.TransportResource;
import com.agripulse.transport.TransportResourceRepository;
import com.agripulse.transport.TransportStatus;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AllocationServiceTest {

  @Autowired private AllocationService allocation;
  @Autowired private CropRepository crops;
  @Autowired private UserRepository users;
  @Autowired private BuyerProfileRepository buyerProfiles;
  @Autowired private BuyerRequirementRepository requirements;
  @Autowired private StorageFacilityRepository storageRepo;
  @Autowired private ProcessingFacilityRepository processingRepo;
  @Autowired private TransportResourceRepository transportRepo;
  @Autowired private MarketRepository markets;
  @Autowired private PasswordEncoder passwordEncoder;

  private Crop tomato;
  private LocalDate from;
  private LocalDate to;

  @BeforeEach
  void setUp() {
    tomato = new Crop();
    tomato.setName("Tomato");
    tomato = crops.save(tomato);
    from = LocalDate.now().minusDays(30);
    to = LocalDate.now().plusDays(60);

    Market alt = new Market();
    alt.setName("AltReg Mandi");
    alt.setRegion("AltReg");
    alt.setAbsorptionMaxTonnes(25);
    markets.save(alt);
  }

  @Test
  void capacityLimitsAndHonestExposure() {
    buildRegion("AllocReg", true, true, true, 100);

    AllocationResult out = allocate("AllocReg", 100);

    List<String> types = out.getEntries().stream().map(AllocationEntry::getResourceType).toList();
    assertEquals(List.of("STORAGE", "PROCESSING", "BUYER_DEMAND", "ALTERNATIVE_MARKET"), types);
    assertEquals(10.0, out.getEntries().get(0).getAllocatedTonnes());
    assertEquals(15.0, out.getEntries().get(1).getAllocatedTonnes());
    assertEquals(20.0, out.getEntries().get(2).getAllocatedTonnes());
    assertEquals(25.0, out.getEntries().get(3).getAllocatedTonnes());
    assertEquals(70.0, out.getTotalAllocatedTonnes());
    assertEquals(30.0, out.getRemainingExposureTonnes());
    assertFalse(out.isFullyAbsorbed());
    // Known rate data produces estimates; buyer demand has none.
    assertEquals(50.0, out.getEntries().get(0).getCostEstimate());
    assertEquals(null, out.getEntries().get(2).getCostEstimate());
  }

  @Test
  void noStorageStillAllocates() {
    buildRegion("NoStoreReg", false, true, true, 100);

    AllocationResult out = allocate("NoStoreReg", 100);

    assertTrue(out.getEntries().stream().noneMatch(e -> e.getResourceType().equals("STORAGE")));
    assertEquals(15.0 + 20.0 + 25.0, out.getTotalAllocatedTonnes());
    assertEquals(40.0, out.getRemainingExposureTonnes());
  }

  @Test
  void noTransportBlocksAlternativeMarkets() {
    buildRegion("NoTransReg", true, true, true, 0);

    AllocationResult out = allocate("NoTransReg", 100);

    assertTrue(
        out.getEntries().stream().noneMatch(e -> e.getResourceType().equals("ALTERNATIVE_MARKET")));
    assertEquals(10.0 + 15.0 + 20.0, out.getTotalAllocatedTonnes());
    assertEquals(55.0, out.getRemainingExposureTonnes());
  }

  @Test
  void incompatibleStorageExcluded() {
    buildRegion("AllocReg2", true, true, true, 100);

    AllocationResult out = allocate("AllocReg2", 100);

    List<AllocationEntry> storageEntries =
        out.getEntries().stream().filter(e -> e.getResourceType().equals("STORAGE")).toList();
    assertEquals(1, storageEntries.size());
    assertEquals("Cold Store", storageEntries.get(0).getResourceName());
  }

  @Test
  void zeroSurplusIsFullyAbsorbed() {
    buildRegion("ZeroReg", true, true, true, 100);

    AllocationResult out = allocate("ZeroReg", 0);

    assertTrue(out.getEntries().isEmpty());
    assertTrue(out.isFullyAbsorbed());
    assertEquals(0.0, out.getRemainingExposureTonnes());
  }

  @Test
  void invalidNegativeSurplusRejected() {
    AllocationRequest request = new AllocationRequest();
    request.setCropId(tomato.getId());
    request.setRegion("AllocReg");
    request.setWindowStart(from);
    request.setWindowEnd(to);
    request.setSurplusTonnes(-5);
    assertThrows(IllegalArgumentException.class, () -> allocation.allocate(request));
  }

  @Test
  void deterministicOutput() {
    buildRegion("DetReg", true, true, true, 100);

    AllocationResult first = allocate("DetReg", 100);
    AllocationResult second = allocate("DetReg", 100);

    assertEquals(first.getEntries().size(), second.getEntries().size());
    for (int i = 0; i < first.getEntries().size(); i++) {
      AllocationEntry a = first.getEntries().get(i);
      AllocationEntry b = second.getEntries().get(i);
      assertEquals(a.getResourceType(), b.getResourceType());
      assertEquals(a.getResourceId(), b.getResourceId());
      assertEquals(a.getAllocatedTonnes(), b.getAllocatedTonnes());
      assertEquals(a.getReason(), b.getReason());
      assertEquals(a.getCostEstimate(), b.getCostEstimate());
    }
    assertEquals(first.getRemainingExposureTonnes(), second.getRemainingExposureTonnes());
  }

  /**
   * @param withStorage includes a compatible 10t store (plus an incompatible wheat-only store)
   * @param withProcessing includes a 15t tomato unit
   * @param withBuyer includes a 20t open requirement
   * @param transportCapacity 0 also means no transport rows
   */
  private void buildRegion(
      String region, boolean withStorage, boolean withProcessing, boolean withBuyer,
      double transportCapacity) {
    if (withStorage) {
      StorageFacility cold = new StorageFacility();
      cold.setName("Cold Store");
      cold.setRegion(region);
      cold.setCapacityTonnes(10);
      cold.setOccupiedTonnes(0);
      cold.setCropCompatibility("Tomato");
      cold.setCostPerDay(5.0);
      storageRepo.save(cold);

      StorageFacility wheatOnly = new StorageFacility();
      wheatOnly.setName("Wheat Only");
      wheatOnly.setRegion(region);
      wheatOnly.setCapacityTonnes(50);
      wheatOnly.setOccupiedTonnes(0);
      wheatOnly.setCropCompatibility("Wheat");
      storageRepo.save(wheatOnly);
    }
    if (withProcessing) {
      ProcessingFacility unit = new ProcessingFacility();
      unit.setName("Tomato Unit");
      unit.setRegion(region);
      unit.setCrop(tomato);
      unit.setCapacityTonnes(15);
      unit.setOccupiedTonnes(0);
      processingRepo.save(unit);
    }
    if (withBuyer) {
      User buyer = new User();
      buyer.setEmail("buyer-" + region + "@test.local");
      buyer.setPasswordHash(passwordEncoder.encode("password123"));
      buyer.setRole(Role.BUYER);
      buyer.setAccountStatus(AccountStatus.ACTIVE);
      buyer = users.save(buyer);
      BuyerProfile profile = new BuyerProfile();
      profile.setUser(buyer);
      profile.setOrgName("buyer-" + region);
      profile.setRegion(region);
      profile = buyerProfiles.save(profile);
      BuyerRequirement requirement = new BuyerRequirement();
      requirement.setBuyer(profile);
      requirement.setCrop(tomato);
      requirement.setQuantityTonnes(20);
      requirement.setRequiredDate(from.plusDays(10));
      requirement.setRegion(region);
      requirement.setStatus(RequirementStatus.OPEN);
      requirement.setDataSource(DataSource.REAL);
      requirements.save(requirement);
    }
    if (transportCapacity > 0) {
      TransportResource transport = new TransportResource();
      transport.setCapacityTonnes(transportCapacity);
      transport.setOriginRegion(region);
      transport.setDestRegion("Mumbai");
      transport.setStatus(TransportStatus.AVAILABLE);
      transport.setCostPerKm(2.0);
      transportRepo.save(transport);
    }
  }

  private AllocationResult allocate(String region, double surplus) {
    AllocationRequest request = new AllocationRequest();
    request.setCropId(tomato.getId());
    request.setRegion(region);
    request.setWindowStart(from);
    request.setWindowEnd(to);
    request.setSurplusTonnes(surplus);
    return allocation.allocate(request);
  }
}
