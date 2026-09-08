package com.agripulse.pressure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agripulse.buyer.BuyerProfile;
import com.agripulse.buyer.BuyerProfileRepository;
import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.demand.BuyerRequirement;
import com.agripulse.demand.BuyerRequirementRepository;
import com.agripulse.demand.DataSource;
import com.agripulse.demand.RequirementStatus;
import com.agripulse.farmer.Farm;
import com.agripulse.farmer.FarmRepository;
import com.agripulse.farmer.FarmerProfile;
import com.agripulse.farmer.FarmerProfileRepository;
import com.agripulse.market.Market;
import com.agripulse.market.MarketObservation;
import com.agripulse.market.MarketObservationRepository;
import com.agripulse.market.MarketRepository;
import com.agripulse.pressure.PressureService.PressureAssessment;
import com.agripulse.processing.ProcessingService;
import com.agripulse.storage.StorageFacility;
import com.agripulse.storage.StorageFacilityRepository;
import com.agripulse.supply.CreateSupplyReportRequest;
import com.agripulse.supply.SupplyReportService;
import com.agripulse.transport.TransportResource;
import com.agripulse.transport.TransportResourceRepository;
import com.agripulse.transport.TransportStatus;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import com.agripulse.user.VerificationStatus;
import java.time.LocalDate;
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
class PressureAssessmentTest {

  @Autowired private PressureService pressure;
  @Autowired private SupplyReportService supplyReports;
  @Autowired private ProcessingService processingService;
  @Autowired private UserRepository users;
  @Autowired private FarmerProfileRepository farmerProfiles;
  @Autowired private FarmRepository farms;
  @Autowired private BuyerProfileRepository buyerProfiles;
  @Autowired private CropRepository crops;
  @Autowired private BuyerRequirementRepository requirements;
  @Autowired private MarketRepository markets;
  @Autowired private MarketObservationRepository observations;
  @Autowired private StorageFacilityRepository storageRepo;
  @Autowired private TransportResourceRepository transportRepo;
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
  }

  @Test
  void balancedSupplyAndDemand() {
    String region = "BalReg";
    Farm farm = verifiedFarm("bal@test.local", region);
    demand(region, 80, 50, 70);
    supply(farm, 150, 170, false);
    storage(region, 200, "Tomato");
    transport(region, 200);
    processingService.register("Unit", region, tomato.getId(), 200, 0, null, null);

    PressureAssessment out = pressure.assess(tomato.getId(), region, from, to);

    assertEquals(PressureBand.MODERATE, out.band());
    assertTrue(reasonText(out).contains("within estimated demand range"));
  }

  @Test
  void oversupplyIsCritical() {
    String region = "OverReg";
    Farm farm = verifiedFarm("over@test.local", region);
    demand(region, 20, 0, 0);
    supply(farm, 55, 65, false);
    supply(farm, 55, 65, false);

    PressureAssessment out = pressure.assess(tomato.getId(), region, from, to);

    assertEquals(PressureBand.CRITICAL, out.band());
    String text = reasonText(out);
    assertTrue(text.contains("above estimated demand range"));
    assertTrue(text.contains("no storage available"));
    assertTrue(text.contains("no transport available"));
  }

  @Test
  void shortageIsLow() {
    String region = "ShortReg";
    Farm farm = verifiedFarm("short@test.local", region);
    demand(region, 100, 0, 0);
    supply(farm, 15, 25, false);
    storage(region, 50, "Tomato");
    transport(region, 50);

    PressureAssessment out = pressure.assess(tomato.getId(), region, from, to);

    assertEquals(PressureBand.LOW, out.band());
    assertTrue(reasonText(out).contains("shortage"));
  }

  @Test
  void noStorageKeepsBandWithReason() {
    String region = "NoStoreReg";
    Farm farm = verifiedFarm("nostore@test.local", region);
    demand(region, 80, 50, 70);
    supply(farm, 150, 170, false);
    transport(region, 200);

    PressureAssessment out = pressure.assess(tomato.getId(), region, from, to);

    assertEquals(PressureBand.MODERATE, out.band());
    assertTrue(reasonText(out).contains("no storage available"));
  }

  @Test
  void insufficientTransportEscalates() {
    String region = "TransReg";
    Farm farm = verifiedFarm("trans@test.local", region);
    demand(region, 80, 50, 70);
    supply(farm, 150, 170, false);
    storage(region, 200, "Tomato");
    transport(region, 30);

    PressureAssessment out = pressure.assess(tomato.getId(), region, from, to);

    assertEquals(PressureBand.HIGH, out.band());
    assertTrue(reasonText(out).contains("constrained"));
  }

  @Test
  void highProcessingCapacityIsNoted() {
    String region = "ProcReg";
    Farm farm = verifiedFarm("proc@test.local", region);
    demand(region, 80, 50, 70);
    supply(farm, 150, 170, false);
    storage(region, 200, "Tomato");
    transport(region, 200);
    processingService.register("Big Unit", region, tomato.getId(), 500, 0, null, null);

    PressureAssessment out = pressure.assess(tomato.getId(), region, from, to);

    assertTrue(reasonText(out).contains("processing capacity covers expected supply"));
  }

  @Test
  void lowConfidenceSupplyCountsLess() {
    String highRegion = "HiConfReg";
    String lowRegion = "LoConfReg";
    Farm highFarm = verifiedFarm("hiconf@test.local", highRegion);
    Farm lowFarm = lowTrustFarm("loconf@test.local", lowRegion);
    demand(highRegion, 80, 50, 70);
    demand(lowRegion, 80, 50, 70);
    supply(highFarm, 150, 170, false);
    supply(lowFarm, 150, 170, true);
    transport(highRegion, 500);
    transport(lowRegion, 500);
    storage(highRegion, 500, "Tomato");
    storage(lowRegion, 500, "Tomato");

    PressureAssessment high = pressure.assess(tomato.getId(), highRegion, from, to);
    PressureAssessment low = pressure.assess(tomato.getId(), lowRegion, from, to);

    assertTrue(low.effectiveSupplyTonnes() < high.effectiveSupplyTonnes());
    assertTrue(low.band().ordinal() < high.band().ordinal());
  }

  @Test
  void missingOptionalResourcesStayModerate() {
    String region = "OptReg";
    Farm farm = verifiedFarm("opt@test.local", region);
    demand(region, 80, 50, 70);
    supply(farm, 150, 170, false);
    transport(region, 200);

    PressureAssessment out = pressure.assess(tomato.getId(), region, from, to);

    assertEquals(PressureBand.MODERATE, out.band());
    String text = reasonText(out);
    assertTrue(text.contains("no storage available"));
    assertTrue(text.contains("no processing capacity"));
  }

  private String reasonText(PressureAssessment out) {
    StringBuilder sb = new StringBuilder();
    out.reasons().forEach(r -> sb.append(r.component()).append(':').append(r.message()).append('\n'));
    return sb.toString();
  }

  private Farm verifiedFarm(String email, String region) {
    User user = newUser(email, Role.FARMER, VerificationStatus.IDENTITY_VERIFIED);
    FarmerProfile profile = new FarmerProfile();
    profile.setUser(user);
    profile.setFullName("Farmer " + email);
    profile.setRegion(region);
    profile.setFpoValidated(true);
    profile = farmerProfiles.save(profile);
    Farm farm = new Farm();
    farm.setFarmer(profile);
    farm.setName("Farm " + email);
    farm.setRegion(region);
    return farms.save(farm);
  }

  private Farm lowTrustFarm(String email, String region) {
    User user = newUser(email, Role.FARMER, VerificationStatus.UNVERIFIED);
    FarmerProfile profile = new FarmerProfile();
    profile.setUser(user);
    profile.setFullName("Farmer " + email);
    profile.setRegion("Elsewhere");
    profile.setFpoValidated(false);
    profile = farmerProfiles.save(profile);
    Farm farm = new Farm();
    farm.setFarmer(profile);
    farm.setName("Farm " + email);
    farm.setRegion(region);
    return farms.save(farm);
  }

  private User newUser(String email, Role role, VerificationStatus status) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode("password123"));
    user.setRole(role);
    user.setAccountStatus(AccountStatus.ACTIVE);
    user.setVerificationStatus(status);
    return users.save(user);
  }

  private void supply(Farm farm, double min, double max, boolean farFuture) {
    CreateSupplyReportRequest request = new CreateSupplyReportRequest();
    request.setFarmId(farm.getId());
    request.setCropId(tomato.getId());
    request.setQuantityMinTonnes(min);
    request.setQuantityMaxTonnes(max);
    if (farFuture) {
      request.setHarvestStart(LocalDate.now().plusDays(400));
      request.setHarvestEnd(LocalDate.now().plusDays(410));
    } else {
      request.setHarvestStart(LocalDate.now());
      request.setHarvestEnd(LocalDate.now().plusDays(30));
    }
    request.setQuality("Good");
    request.setRegion(farm.getRegion());
    supplyReports.create(request, farm.getFarmer().getUser().getEmail());
  }

  private void demand(String region, double confirmedTonnes, double obsMin, double obsMax) {
    User buyer = newUser("buyer-" + region + "@test.local", Role.BUYER, VerificationStatus.UNVERIFIED);
    BuyerProfile profile = new BuyerProfile();
    profile.setUser(buyer);
    profile.setOrgName("buyer-" + region);
    profile.setRegion(region);
    profile = buyerProfiles.save(profile);
    BuyerRequirement requirement = new BuyerRequirement();
    requirement.setBuyer(profile);
    requirement.setCrop(tomato);
    requirement.setQuantityTonnes(confirmedTonnes);
    requirement.setRequiredDate(from.plusDays(10));
    requirement.setRegion(region);
    requirement.setStatus(RequirementStatus.OPEN);
    requirement.setDataSource(DataSource.REAL);
    requirements.save(requirement);

    if (obsMax > 0) {
      Market market = new Market();
      market.setName("Mandi " + region);
      market.setRegion(region);
      market = markets.save(market);
      MarketObservation observation = new MarketObservation();
      observation.setMarket(market);
      observation.setCrop(tomato);
      observation.setPeriodStart(to.minusDays(60));
      observation.setPeriodEnd(to.minusDays(30));
      observation.setAbsorptionMinTonnes(obsMin);
      observation.setAbsorptionMaxTonnes(obsMax);
      observation.setDataSource(DataSource.REAL);
      observations.save(observation);
    }
  }

  private void storage(String region, double capacity, String compatibility) {
    StorageFacility facility = new StorageFacility();
    facility.setName("Store " + region);
    facility.setRegion(region);
    facility.setCapacityTonnes(capacity);
    facility.setOccupiedTonnes(0);
    facility.setCropCompatibility(compatibility);
    storageRepo.save(facility);
  }

  private void transport(String region, double capacity) {
    TransportResource resource = new TransportResource();
    resource.setCapacityTonnes(capacity);
    resource.setOriginRegion(region);
    resource.setDestRegion("Mumbai");
    resource.setStatus(TransportStatus.AVAILABLE);
    transportRepo.save(resource);
  }
}
