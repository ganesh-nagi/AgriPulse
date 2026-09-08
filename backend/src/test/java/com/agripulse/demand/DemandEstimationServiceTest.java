package com.agripulse.demand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agripulse.buyer.BuyerProfile;
import com.agripulse.buyer.BuyerProfileRepository;
import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.market.Market;
import com.agripulse.market.MarketObservation;
import com.agripulse.market.MarketObservationRepository;
import com.agripulse.market.MarketRepository;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
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
class DemandEstimationServiceTest {

  @Autowired private DemandEstimationService service;
  @Autowired private BuyerRequirementRepository requirements;
  @Autowired private BuyerProfileRepository buyerProfiles;
  @Autowired private MarketObservationRepository observations;
  @Autowired private MarketRepository markets;
  @Autowired private CropRepository crops;
  @Autowired private UserRepository users;
  @Autowired private PasswordEncoder passwordEncoder;

  private Crop tomato;
  private Market mandi;
  private LocalDate from;
  private LocalDate to;

  @BeforeEach
  void setUp() {
    tomato = new Crop();
    tomato.setName("Tomato");
    tomato = crops.save(tomato);
    mandi = new Market();
    mandi.setName("Nashik Mandi");
    mandi.setRegion("Nashik");
    mandi = markets.save(mandi);
    from = LocalDate.now().minusDays(30);
    to = LocalDate.now().plusDays(60);
  }

  @Test
  void confirmedDemandWithoutHistory() {
    BuyerProfile buyer = newBuyer("buyer1@test.local");
    addRequirement(buyer, 40, from.plusDays(10));

    DemandEstimateResponse out = service.estimate(tomato.getId(), "Nashik", from, to);

    assertEquals(40.0, out.getConfirmedDemandTonnes());
    assertEquals(1, out.getConfirmedBuyerCount());
    assertEquals(0.0, out.getAbsorptionMinTonnes());
    assertEquals(0.0, out.getAbsorptionMaxTonnes());
    assertEquals(40.0, out.getEstimatedMinTonnes());
    assertEquals(40.0, out.getEstimatedMaxTonnes());
    assertEquals(DataSource.ESTIMATED, out.getDataSource());
  }

  @Test
  void historicalObservationsShapeEstimate() {
    BuyerProfile buyer = newBuyer("buyer1@test.local");
    addRequirement(buyer, 40, from.plusDays(10));
    addObservation(50, 70, to.minusDays(60), to.minusDays(30), DataSource.REAL);
    addObservation(55, 75, to.minusDays(30), to.minusDays(1), DataSource.REAL);

    DemandEstimateResponse out = service.estimate(tomato.getId(), "Nashik", from, to);

    assertEquals(50.0, out.getAbsorptionMinTonnes());
    assertEquals(75.0, out.getAbsorptionMaxTonnes());
    assertEquals(90.0, out.getEstimatedMinTonnes());
    assertEquals(115.0, out.getEstimatedMaxTonnes());
    assertTrue(out.getConfidence() <= 95.0);
  }

  @Test
  void noDataGivesLowConfidenceRange() {
    DemandEstimateResponse out = service.estimate(tomato.getId(), "Nashik", from, to);

    assertEquals(0.0, out.getConfirmedDemandTonnes());
    assertEquals(0, out.getObservationCount());
    assertEquals(0.0, out.getEstimatedMinTonnes());
    assertEquals(0.0, out.getEstimatedMaxTonnes());
    assertTrue(out.getConfidence() < 40.0);
  }

  @Test
  void multipleBuyersRaiseConfidence() {
    BuyerProfile buyer1 = newBuyer("buyer1@test.local");
    BuyerProfile buyer2 = newBuyer("buyer2@test.local");
    addRequirement(buyer1, 40, from.plusDays(10));
    DemandEstimateResponse single = service.estimate(tomato.getId(), "Nashik", from, to);

    addRequirement(buyer2, 30, from.plusDays(20));
    DemandEstimateResponse multi = service.estimate(tomato.getId(), "Nashik", from, to);

    assertEquals(70.0, multi.getConfirmedDemandTonnes());
    assertEquals(2, multi.getConfirmedBuyerCount());
    assertTrue(multi.getConfidence() > single.getConfidence());
  }

  @Test
  void timeWindowFiltering() {
    BuyerProfile buyer = newBuyer("buyer1@test.local");
    addRequirement(buyer, 40, from.plusDays(10));
    addRequirement(buyer, 999, to.plusDays(30)); // outside window
    addObservation(50, 70, to.minusDays(60), to.minusDays(30), DataSource.REAL);
    // Stale observation: period ended more than 24 months before window end.
    addObservation(5, 9, to.minusDays(800), to.minusDays(760), DataSource.REAL);

    DemandEstimateResponse out = service.estimate(tomato.getId(), "Nashik", from, to);

    assertEquals(40.0, out.getConfirmedDemandTonnes());
    assertEquals(1, out.getObservationCount());
    assertEquals(50.0, out.getAbsorptionMinTonnes());
    assertEquals(70.0, out.getAbsorptionMaxTonnes());
  }

  @Test
  void provenanceLabelsEverySignal() {
    BuyerProfile buyer = newBuyer("buyer1@test.local");
    addRequirement(buyer, 40, from.plusDays(10));
    addObservation(50, 70, to.minusDays(60), to.minusDays(30), DataSource.SIMULATED);

    DemandEstimateResponse out = service.estimate(tomato.getId(), "Nashik", from, to);

    Map<String, DemandEstimateResponse.SignalProvenance> bySignal =
        out.getProvenance().stream()
            .collect(Collectors.toMap(DemandEstimateResponse.SignalProvenance::signal, p -> p));
    assertTrue(bySignal.get("CONFIRMED").sources().contains(DataSource.REAL));
    assertTrue(bySignal.get("OBSERVED").sources().contains(DataSource.SIMULATED));
    assertTrue(bySignal.get("ESTIMATED").sources().contains(DataSource.ESTIMATED));
  }

  private BuyerProfile newBuyer(String email) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode("password123"));
    user.setRole(Role.BUYER);
    user.setAccountStatus(AccountStatus.ACTIVE);
    user = users.save(user);
    BuyerProfile profile = new BuyerProfile();
    profile.setUser(user);
    profile.setOrgName(email.split("@")[0]);
    profile.setRegion("Nashik");
    return buyerProfiles.save(profile);
  }

  private void addRequirement(BuyerProfile buyer, double tonnes, LocalDate requiredDate) {
    BuyerRequirement requirement = new BuyerRequirement();
    requirement.setBuyer(buyer);
    requirement.setCrop(tomato);
    requirement.setQuantityTonnes(tonnes);
    requirement.setRequiredDate(requiredDate);
    requirement.setRegion("Nashik");
    requirement.setStatus(RequirementStatus.OPEN);
    requirement.setDataSource(DataSource.REAL);
    requirements.save(requirement);
  }

  private void addObservation(
      double min, double max, LocalDate start, LocalDate end, DataSource source) {
    MarketObservation observation = new MarketObservation();
    observation.setMarket(mandi);
    observation.setCrop(tomato);
    observation.setPeriodStart(start);
    observation.setPeriodEnd(end);
    observation.setAbsorptionMinTonnes(min);
    observation.setAbsorptionMaxTonnes(max);
    observation.setDataSource(source);
    observations.save(observation);
  }
}
