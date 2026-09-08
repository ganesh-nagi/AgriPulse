package com.agripulse.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agripulse.buyer.BuyerProfile;
import com.agripulse.buyer.BuyerProfileRepository;
import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.demand.BuyerRequirement;
import com.agripulse.demand.BuyerRequirementRepository;
import com.agripulse.demand.DataSource;
import com.agripulse.demand.DemandEstimateRepository;
import com.agripulse.demand.RequirementStatus;
import com.agripulse.farmer.Farm;
import com.agripulse.farmer.FarmRepository;
import com.agripulse.farmer.FarmerProfile;
import com.agripulse.farmer.FarmerProfileRepository;
import com.agripulse.market.Market;
import com.agripulse.market.MarketObservation;
import com.agripulse.market.MarketObservationRepository;
import com.agripulse.market.MarketRepository;
import com.agripulse.pressure.PressureBand;
import com.agripulse.pressure.PressureService.PressureAssessment;
import com.agripulse.processing.ProcessingFacilityRepository;
import com.agripulse.storage.StorageFacility;
import com.agripulse.storage.StorageFacilityRepository;
import com.agripulse.supply.CreateSupplyReportRequest;
import com.agripulse.supply.SupplyReportRepository;
import com.agripulse.supply.SupplyReportService;
import com.agripulse.transport.TransportResource;
import com.agripulse.transport.TransportResourceRepository;
import com.agripulse.transport.TransportStatus;
import com.agripulse.trust.TrustScoreRepository;
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
class ScenarioSimulationServiceTest {

  @Autowired private ScenarioSimulationService simulator;
  @Autowired private SupplyReportService supplyReports;
  @Autowired private SupplyReportRepository reports;
  @Autowired private TrustScoreRepository trustScores;
  @Autowired private DemandEstimateRepository estimates;
  @Autowired private StorageFacilityRepository storageRepo;
  @Autowired private TransportResourceRepository transportRepo;
  @Autowired private ProcessingFacilityRepository processingRepo;
  @Autowired private UserRepository users;
  @Autowired private FarmerProfileRepository farmerProfiles;
  @Autowired private FarmRepository farms;
  @Autowired private BuyerProfileRepository buyerProfiles;
  @Autowired private CropRepository crops;
  @Autowired private BuyerRequirementRepository requirements;
  @Autowired private MarketRepository markets;
  @Autowired private MarketObservationRepository observations;
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

    // Balanced baseline in ScenReg: effective ~136 t vs estimate 130-150 t.
    User farmer = newUser("scen@test.local", Role.FARMER, VerificationStatus.IDENTITY_VERIFIED);
    FarmerProfile profile = new FarmerProfile();
    profile.setUser(farmer);
    profile.setFullName("Scenario Farmer");
    profile.setRegion("ScenReg");
    profile.setFpoValidated(true);
    profile = farmerProfiles.save(profile);
    Farm farm = new Farm();
    farm.setFarmer(profile);
    farm.setName("Scenario Farm");
    farm.setRegion("ScenReg");
    farm = farms.save(farm);
    CreateSupplyReportRequest request = new CreateSupplyReportRequest();
    request.setFarmId(farm.getId());
    request.setCropId(tomato.getId());
    request.setQuantityMinTonnes(150);
    request.setQuantityMaxTonnes(170);
    request.setHarvestStart(LocalDate.now());
    request.setHarvestEnd(LocalDate.now().plusDays(30));
    request.setQuality("Good");
    request.setRegion("ScenReg");
    supplyReports.create(request, farmer.getEmail());

    User buyer = newUser("scenbuyer@test.local", Role.BUYER, VerificationStatus.UNVERIFIED);
    BuyerProfile buyerProfile = new BuyerProfile();
    buyerProfile.setUser(buyer);
    buyerProfile.setOrgName("scenbuyer");
    buyerProfile.setRegion("ScenReg");
    buyerProfile = buyerProfiles.save(buyerProfile);
    BuyerRequirement requirement = new BuyerRequirement();
    requirement.setBuyer(buyerProfile);
    requirement.setCrop(tomato);
    requirement.setQuantityTonnes(80);
    requirement.setRequiredDate(from.plusDays(10));
    requirement.setRegion("ScenReg");
    requirement.setStatus(RequirementStatus.OPEN);
    requirement.setDataSource(DataSource.REAL);
    requirements.save(requirement);

    Market market = new Market();
    market.setName("ScenReg Mandi");
    market.setRegion("ScenReg");
    market = markets.save(market);
    MarketObservation observation = new MarketObservation();
    observation.setMarket(market);
    observation.setCrop(tomato);
    observation.setPeriodStart(to.minusDays(60));
    observation.setPeriodEnd(to.minusDays(30));
    observation.setAbsorptionMinTonnes(50);
    observation.setAbsorptionMaxTonnes(70);
    observation.setDataSource(DataSource.REAL);
    observations.save(observation);

    StorageFacility storage = new StorageFacility();
    storage.setName("ScenReg Store");
    storage.setRegion("ScenReg");
    storage.setCapacityTonnes(200);
    storage.setOccupiedTonnes(0);
    storage.setCropCompatibility("Tomato");
    storageRepo.save(storage);

    TransportResource transport = new TransportResource();
    transport.setCapacityTonnes(200);
    transport.setOriginRegion("ScenReg");
    transport.setDestRegion("Mumbai");
    transport.setStatus(TransportStatus.AVAILABLE);
    transportRepo.save(transport);
  }

  @Test
  void scenarioDoesNotMutateProduction() {
    long reportCount = reports.count();
    long scoreCount = trustScores.count();
    long estimateCount = estimates.count();
    long storageCount = storageRepo.count();
    long transportCount = transportRepo.count();
    long processingCount = processingRepo.count();

    ScenarioInput in = baseInput();
    in.setSupplyDeltaTonnes(40);
    in.setDemandDeltaTonnes(20);
    in.setStorageOverrideTonnes(0.0);
    in.setTransportOverrideTonnes(10.0);
    simulator.simulate(new Scenario("stress", in));

    assertEquals(reportCount, reports.count());
    assertEquals(scoreCount, trustScores.count());
    assertEquals(estimateCount, estimates.count());
    assertEquals(storageCount, storageRepo.count());
    assertEquals(transportCount, transportRepo.count());
    assertEquals(processingCount, processingRepo.count());
  }

  @Test
  void supplyIncreaseEscalatesBand() {
    ScenarioInput in = baseInput();
    in.setSupplyDeltaTonnes(40);
    ScenarioResult out = simulator.simulate(new Scenario("+40t supply", in));

    assertEquals(PressureBand.MODERATE, out.getBaseline().band());
    assertEquals(PressureBand.HIGH, out.getScenario().band());
    assertEquals(out.getBaseline().effectiveSupplyTonnes() + 40.0,
        out.getScenario().effectiveSupplyTonnes(), 0.0001);
  }

  @Test
  void noStorageKeepsBandWithReason() {
    ScenarioInput in = baseInput();
    in.setStorageOverrideTonnes(0.0);
    ScenarioResult out = simulator.simulate(new Scenario("no storage", in));

    assertEquals(PressureBand.MODERATE, out.getScenario().band());
    assertTrue(reasonText(out.getScenario()).contains("no storage available"));
  }

  @Test
  void transportReductionConstrains() {
    ScenarioInput in = baseInput();
    in.setTransportOverrideTonnes(30.0);
    ScenarioResult out = simulator.simulate(new Scenario("less transport", in));

    assertEquals(PressureBand.HIGH, out.getScenario().band());
    assertTrue(reasonText(out.getScenario()).contains("constrained"));
  }

  @Test
  void demandIncreaseCreatesShortage() {
    ScenarioInput in = baseInput();
    in.setDemandDeltaTonnes(100);
    ScenarioResult out = simulator.simulate(new Scenario("more demand", in));

    assertEquals(PressureBand.LOW, out.getScenario().band());
    assertTrue(reasonText(out.getScenario()).contains("shortage"));
  }

  @Test
  void invalidNegativeValuesRejected() {
    ScenarioInput storage = baseInput();
    storage.setStorageOverrideTonnes(-1.0);
    assertThrows(
        IllegalArgumentException.class, () -> simulator.simulate(new Scenario("bad", storage)));

    ScenarioInput transport = baseInput();
    transport.setTransportOverrideTonnes(-1.0);
    assertThrows(
        IllegalArgumentException.class, () -> simulator.simulate(new Scenario("bad", transport)));

    ScenarioInput processing = baseInput();
    processing.setProcessingOverrideTonnes(-1.0);
    assertThrows(
        IllegalArgumentException.class, () -> simulator.simulate(new Scenario("bad", processing)));

    ScenarioInput window = baseInput();
    window.setWindowStart(to.plusDays(1));
    window.setWindowEnd(to);
    assertThrows(
        IllegalArgumentException.class, () -> simulator.simulate(new Scenario("bad", window)));
  }

  @Test
  void repeatedIdenticalScenarioIsIdentical() {
    ScenarioInput first = baseInput();
    first.setSupplyDeltaTonnes(25);
    first.setDemandDeltaTonnes(-10);
    first.setTransportOverrideTonnes(50.0);
    ScenarioResult one = simulator.simulate(new Scenario("repeat", first));

    ScenarioInput second = baseInput();
    second.setSupplyDeltaTonnes(25);
    second.setDemandDeltaTonnes(-10);
    second.setTransportOverrideTonnes(50.0);
    ScenarioResult two = simulator.simulate(new Scenario("repeat", second));

    assertEquals(one.getScenario().band(), two.getScenario().band());
    assertEquals(one.getScenario().effectiveSupplyTonnes(), two.getScenario().effectiveSupplyTonnes());
    assertEquals(one.getScenario().estimatedMinTonnes(), two.getScenario().estimatedMinTonnes());
    assertEquals(one.getScenario().estimatedMaxTonnes(), two.getScenario().estimatedMaxTonnes());
    assertEquals(reasonText(one.getScenario()), reasonText(two.getScenario()));
  }

  private ScenarioInput baseInput() {
    ScenarioInput in = new ScenarioInput();
    in.setCropId(tomato.getId());
    in.setRegion("ScenReg");
    in.setWindowStart(from);
    in.setWindowEnd(to);
    return in;
  }

  private String reasonText(PressureAssessment assessment) {
    StringBuilder sb = new StringBuilder();
    assessment.reasons().forEach(r -> sb.append(r.component()).append(':').append(r.message()).append('\n'));
    return sb.toString();
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
}
