package com.agripulse.demo;

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
import com.agripulse.pressure.PressureService;
import com.agripulse.processing.ProcessingFacility;
import com.agripulse.processing.ProcessingFacilityRepository;
import com.agripulse.storage.StorageFacility;
import com.agripulse.storage.StorageFacilityRepository;
import com.agripulse.supply.CreateSupplyReportRequest;
import com.agripulse.supply.SupplyEvidence;
import com.agripulse.supply.SupplyEvidenceRepository;
import com.agripulse.supply.SupplyReportRepository;
import com.agripulse.supply.SupplyReportService;
import com.agripulse.transport.TransportResource;
import com.agripulse.transport.TransportResourceRepository;
import com.agripulse.transport.TransportStatus;
import com.agripulse.trust.TrustScore;
import com.agripulse.trust.TrustScoreRepository;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import com.agripulse.user.VerificationStatus;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic synthetic demo data (demo profile only, never production).
 * All dates are relative to today so scenarios stay valid. No randomness:
 * every value derives from fixed loops. Imported-style rows are labelled
 * SIMULATED; computed figures remain ESTIMATED via the normal services.
 */
@Service
@Profile("demo")
public class DemoDataService {

  static final String PASSWORD = "demo1234";
  static final String[] REGIONS = {"Nashik", "Pune", "Ahmednagar", "Solapur", "Kolhapur"};
  static final double[][] REGION_COORDS = {
    {19.9975, 73.7898},
    {18.5204, 73.8567},
    {19.0948, 74.7480},
    {17.6599, 75.9064},
    {16.7050, 74.2433}
  };

  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwordEncoder;
  private final UserRepository users;
  private final FarmerProfileRepository farmerProfiles;
  private final FarmRepository farms;
  private final CropRepository crops;
  private final SupplyReportService supplyReports;
  private final SupplyReportRepository reports;
  private final SupplyEvidenceRepository evidence;
  private final TrustScoreRepository trustScores;
  private final BuyerProfileRepository buyerProfiles;
  private final BuyerRequirementRepository requirements;
  private final MarketRepository markets;
  private final MarketObservationRepository observations;
  private final StorageFacilityRepository storageRepo;
  private final TransportResourceRepository transportRepo;
  private final ProcessingFacilityRepository processingRepo;
  private final PressureService pressure;

  public DemoDataService(
      JdbcTemplate jdbc,
      PasswordEncoder passwordEncoder,
      UserRepository users,
      FarmerProfileRepository farmerProfiles,
      FarmRepository farms,
      CropRepository crops,
      SupplyReportService supplyReports,
      SupplyReportRepository reports,
      SupplyEvidenceRepository evidence,
      TrustScoreRepository trustScores,
      BuyerProfileRepository buyerProfiles,
      BuyerRequirementRepository requirements,
      MarketRepository markets,
      MarketObservationRepository observations,
      StorageFacilityRepository storageRepo,
      TransportResourceRepository transportRepo,
      ProcessingFacilityRepository processingRepo,
      PressureService pressure) {
    this.jdbc = jdbc;
    this.passwordEncoder = passwordEncoder;
    this.users = users;
    this.farmerProfiles = farmerProfiles;
    this.farms = farms;
    this.crops = crops;
    this.supplyReports = supplyReports;
    this.reports = reports;
    this.evidence = evidence;
    this.trustScores = trustScores;
    this.buyerProfiles = buyerProfiles;
    this.requirements = requirements;
    this.markets = markets;
    this.observations = observations;
    this.storageRepo = storageRepo;
    this.transportRepo = transportRepo;
    this.processingRepo = processingRepo;
    this.pressure = pressure;
  }

  /** Wipe demo tables and rebuild the HEALTHY base state. */
  @Transactional
  public DemoSummary reset() {
    jdbc.execute(
        "TRUNCATE supply_evidence, trust_scores, supply_reports, verification_records,"
            + " farmer_profiles, farms, buyer_requirements, buyer_profiles, fpo_profiles,"
            + " demand_estimates, market_observations, markets, storage_facilities,"
            + " transport_resources, processing_facilities, refresh_tokens, notifications,"
            + " audit_log, scenarios, users RESTART IDENTITY CASCADE");
    buildBase();
    return summary("healthy", "Supply and demand approximately balanced.");
  }

  @Transactional
  public DemoSummary applyScenario(String name) {
    reset();
    Map<String, String> notes = new LinkedHashMap<>();
    switch (name) {
      case "high-supply" -> {
        Crop tomato = tomato();
        for (int i = 0; i < 8; i++) {
          User farmer = users.findByEmail("farmer%03d@demo.local".formatted(11 + i)).orElseThrow();
          Farm farm = farmOf(farmer);
          var response =
              supplyReports.create(
                  reportRequest(farm, tomato, 45, 55, "Good"), farmer.getEmail());
          attachEvidence(response.getId(), "demo-surge-" + i);
        }
        notes.put("added", "8 verified reports of 45-55t with evidence");
      }
      case "suspicious" -> {
        Crop tomato = tomato();
        User farmer = users.findByEmail("farmer001@demo.local").orElseThrow();
        Farm farm = farmOf(farmer);
        supplyReports.create(reportRequest(farm, tomato, 3, 4, "Standard"), farmer.getEmail());
        var response =
            supplyReports.create(reportRequest(farm, tomato, 48, 52, "Standard"), farmer.getEmail());
        TrustScore score =
            trustScores.findBySupplyReportId(response.getId()).orElseThrow();
        notes.put("reportId", String.valueOf(response.getId()));
        notes.put("trustLevel", String.valueOf(score.getLevel()));
        notes.put("trustScore", "%.1f".formatted(score.getScore()));
        notes.put("requiresReview", String.valueOf(score.isRequiresReview()));
        notes.put(
            "impact",
            "50t report weighs %.1f t effective vs %.1f t had it carried typical trust"
                .formatted(
                    50 * score.getScore() / 100.0,
                    50 * typicalWeight(farmer.getId(), response.getId())));
      }
      case "no-storage" -> {
        storageRepo.findAll().forEach(s -> s.setOccupiedTonnes(s.getCapacityTonnes()));
        notes.put("storage", "all facilities full; buyers, processing, markets and transport continue");
      }
      case "transport-shortage" -> {
        List<TransportResource> fleet = transportRepo.findAll();
        for (int i = 0; i < 8 && i < fleet.size(); i++) {
          fleet.get(i).setStatus(TransportStatus.MAINTENANCE);
        }
        notes.put("transport", "8 of 10 resources in maintenance; 2 remain available");
      }
      case "strong-demand" -> {
        addRequirement(buyerProfile("buyer01@demo.local"), tomato(), "Nashik", 300);
        notes.put("demand", "extra SIMULATED requirement of 300t in Nashik");
      }
      case "healthy" -> notes.put("state", "base balanced state");
      default -> throw new IllegalArgumentException("Unknown scenario: " + name);
    }
    DemoSummary base = summary(name, "Scenario applied on a fresh balanced base.");
    return new DemoSummary(
        base.name(),
        base.description(),
        base.farmers(),
        base.buyers(),
        base.markets(),
        base.storageFacilities(),
        base.transports(),
        base.pressureBand(),
        base.effectiveSupplyTonnes(),
        base.estimatedMinTonnes(),
        base.estimatedMaxTonnes(),
        notes);
  }

  private void buildBase() {
    Crop tomato = tomato();
    LocalDate today = LocalDate.now();
    String hash = passwordEncoder.encode(PASSWORD);

    User admin = new User();
    admin.setEmail("admin@demo.local");
    admin.setPasswordHash(hash);
    admin.setRole(Role.ADMIN);
    admin.setAccountStatus(AccountStatus.ACTIVE);
    admin.setVerificationStatus(VerificationStatus.IDENTITY_VERIFIED);
    users.save(admin);

    for (int i = 1; i <= 100; i++) {
      String region = REGIONS[(i - 1) / 20];
      User farmer = new User();
      farmer.setEmail("farmer%03d@demo.local".formatted(i));
      farmer.setPasswordHash(hash);
      farmer.setRole(Role.FARMER);
      farmer.setAccountStatus(AccountStatus.ACTIVE);
      farmer.setVerificationStatus(
          i % 10 == 0
              ? VerificationStatus.UNVERIFIED
              : (i % 3 == 0 ? VerificationStatus.PHONE_VERIFIED : VerificationStatus.IDENTITY_VERIFIED));
      users.save(farmer);

      FarmerProfile profile = new FarmerProfile();
      profile.setUser(farmer);
      profile.setFullName("Demo Farmer " + i);
      profile.setRegion(region);
      profile.setFpoValidated(i % 4 == 0);
      farmerProfiles.save(profile);

      Farm farm = new Farm();
      farm.setFarmer(profile);
      farm.setName("Demo Farm " + i);
      farm.setRegion(region);
      farms.save(farm);

      double min = 2 + ((i - 1) % 3);
      var response =
          supplyReports.create(
              reportRequest(farm, tomato, min, min + 2, "Standard"), farmer.getEmail());
      if (i % 2 == 0) {
        attachEvidence(response.getId(), "demo-photo-" + i);
      }
    }

    for (int r = 0; r < REGIONS.length; r++) {
      String region = REGIONS[r];
      double effective = pressure.effectiveSupply(tomato.getId(), region);
      double confirmedTotal = round1(effective * 0.45);
      for (int j = 1; j <= 2; j++) {
        int buyerIndex = r * 2 + j;
        User buyer = new User();
        buyer.setEmail("buyer%02d@demo.local".formatted(buyerIndex));
        buyer.setPasswordHash(hash);
        buyer.setRole(Role.BUYER);
        buyer.setAccountStatus(AccountStatus.ACTIVE);
        users.save(buyer);
        BuyerProfile profile = new BuyerProfile();
        profile.setUser(buyer);
        profile.setOrgName("Demo Buyer " + buyerIndex);
        profile.setRegion(region);
        buyerProfiles.save(profile);
        BuyerRequirement requirement = new BuyerRequirement();
        requirement.setBuyer(profile);
        requirement.setCrop(tomato);
        requirement.setQuantityTonnes(round1(confirmedTotal * (j == 1 ? 0.6 : 0.4)));
        requirement.setQuality("Standard");
        requirement.setRequiredDate(today.plusDays(30));
        requirement.setRegion(region);
        requirement.setStatus(RequirementStatus.OPEN);
        requirement.setDataSource(DataSource.SIMULATED);
        requirements.save(requirement);
      }

      Market market = new Market();
      market.setName(region + " Mandi");
      market.setRegion(region);
      market.setLatitude(REGION_COORDS[r][0]);
      market.setLongitude(REGION_COORDS[r][1]);
      market.setAbsorptionMinTonnes(15 + r * 5);
      market.setAbsorptionMaxTonnes(25 + r * 5);
      market.setMarketType("MANDI");
      market = markets.save(market);
      double absMin = round1(effective * 0.40);
      double absMax = round1(effective * 0.65);
      for (int p = 0; p < 2; p++) {
        MarketObservation observation = new MarketObservation();
        observation.setMarket(market);
        observation.setCrop(tomato);
        observation.setPeriodStart(today.minusDays(75 - p * 30));
        observation.setPeriodEnd(today.minusDays(45 - p * 30));
        observation.setAbsorptionMinTonnes(absMin);
        observation.setAbsorptionMaxTonnes(absMax);
        observation.setDataSource(DataSource.SIMULATED);
        observations.save(observation);
      }
    }

    double[][] stores = {{500, 120}, {300, 50}, {200, 0}};
    for (int s = 0; s < 3; s++) {
      StorageFacility facility = new StorageFacility();
      facility.setName(REGIONS[s] + " Cold Store");
      facility.setRegion(REGIONS[s]);
      facility.setLatitude(REGION_COORDS[s][0] + 0.008);
      facility.setLongitude(REGION_COORDS[s][1] + 0.001);
      facility.setCapacityTonnes(stores[s][0]);
      facility.setOccupiedTonnes(stores[s][1]);
      facility.setCropCompatibility("Tomato");
      facility.setAvailableFrom(today.minusDays(30));
      facility.setAvailableTo(today.plusDays(180));
      storageRepo.save(facility);
    }

    for (int t = 0; t < 10; t++) {
      TransportResource resource = new TransportResource();
      resource.setCapacityTonnes(25 + t * 5);
      resource.setOriginRegion(REGIONS[t / 2]);
      resource.setDestRegion("Mumbai");
      resource.setStatus(TransportStatus.AVAILABLE);
      resource.setCostPerKm(2.0);
      resource.setAvailableFrom(today.minusDays(30));
      resource.setAvailableTo(today.plusDays(180));
      transportRepo.save(resource);
    }

    double[] processingCaps = {100, 60, 40};
    for (int p = 0; p < 3; p++) {
      ProcessingFacility unit = new ProcessingFacility();
      unit.setName(REGIONS[p] + " Grading Unit");
      unit.setRegion(REGIONS[p]);
      unit.setCrop(tomato);
      unit.setCapacityTonnes(processingCaps[p]);
      unit.setOccupiedTonnes(processingCaps[p] * 0.2);
      unit.setLatitude(REGION_COORDS[p][0] - 0.007);
      unit.setLongitude(REGION_COORDS[p][1] - 0.009);
      unit.setAvailableFrom(today.minusDays(30));
      unit.setAvailableTo(today.plusDays(180));
      processingRepo.save(unit);
    }
  }

  private DemoSummary summary(String name, String description) {
    LocalDate today = LocalDate.now();
    var assessment =
        pressure.assess(tomato().getId(), "Nashik", today.minusDays(30), today.plusDays(60));
    return new DemoSummary(
        name,
        description,
        100,
        10,
        (int) markets.count(),
        (int) storageRepo.count(),
        (int) transportRepo.count(),
        assessment.band().name(),
        round1(assessment.effectiveSupplyTonnes()),
        round1(assessment.estimatedMinTonnes()),
        round1(assessment.estimatedMaxTonnes()),
        Map.of());
  }

  private Crop tomato() {
    return crops.findByName("Tomato").orElseThrow(() -> new IllegalStateException("Tomato missing"));
  }

  private BuyerProfile buyerProfile(String email) {
    User buyer = users.findByEmail(email).orElseThrow();
    return buyerProfiles.findByUserId(buyer.getId()).orElseThrow();
  }

  private CreateSupplyReportRequest reportRequest(
      Farm farm, Crop crop, double min, double max, String quality) {
    LocalDate today = LocalDate.now();
    CreateSupplyReportRequest request = new CreateSupplyReportRequest();
    request.setFarmId(farm.getId());
    request.setCropId(crop.getId());
    request.setQuantityMinTonnes(min);
    request.setQuantityMaxTonnes(max);
    request.setHarvestStart(today.plusDays(7));
    request.setHarvestEnd(today.plusDays(37));
    request.setQuality(quality);
    request.setRegion(farm.getRegion());
    return request;
  }

  private void attachEvidence(Long reportId, String reference) {
    var report = reports.findById(reportId).orElseThrow();
    SupplyEvidence item = new SupplyEvidence();
    item.setSupplyReport(report);
    item.setEvidenceType("PHOTO");
    item.setReference(reference);
    evidence.save(item);
    // Re-score so the evidence signal counts (scoring runs at create time).
    supplyReports.rescoreReporterReports(report.getReporter().getId());
  }

  private Farm farmOf(User farmer) {
    FarmerProfile profile =
        farmerProfiles.findByUserId(farmer.getId()).orElseThrow();
    return farms.findByFarmerId(profile.getId()).stream().findFirst().orElseThrow();
  }

  private void addRequirement(BuyerProfile profile, Crop crop, String region, double tonnes) {
    BuyerRequirement requirement = new BuyerRequirement();
    requirement.setBuyer(profile);
    requirement.setCrop(crop);
    requirement.setQuantityTonnes(tonnes);
    requirement.setQuality("Standard");
    requirement.setRequiredDate(LocalDate.now().plusDays(30));
    requirement.setRegion(region);
    requirement.setStatus(RequirementStatus.OPEN);
    requirement.setDataSource(DataSource.SIMULATED);
    requirements.save(requirement);
  }

  private double typicalWeight(Long reporterId, Long excludeReportId) {
    return reports.findByReporterId(reporterId).stream()
        .filter(r -> !r.getId().equals(excludeReportId))
        .map(r -> trustScores.findBySupplyReportId(r.getId()))
        .filter(java.util.Optional::isPresent)
        .mapToDouble(ts -> ts.get().getScore() / 100.0)
        .average()
        .orElse(0.85);
  }

  private static double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  public record DemoSummary(
      String name,
      String description,
      int farmers,
      int buyers,
      int markets,
      int storageFacilities,
      int transports,
      String pressureBand,
      double effectiveSupplyTonnes,
      double estimatedMinTonnes,
      double estimatedMaxTonnes,
      Map<String, String> notes) {}
}
