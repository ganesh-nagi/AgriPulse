package com.agripulse.supply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.farmer.Farm;
import com.agripulse.farmer.FarmRepository;
import com.agripulse.farmer.FarmerProfile;
import com.agripulse.farmer.FarmerProfileRepository;
import com.agripulse.trust.TrustLevel;
import com.agripulse.trust.TrustScore;
import com.agripulse.trust.TrustScoreRepository;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import com.agripulse.user.VerificationStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TrustReportIntegrationTest {

  @Autowired private SupplyReportService service;
  @Autowired private UserRepository users;
  @Autowired private FarmerProfileRepository profiles;
  @Autowired private FarmRepository farms;
  @Autowired private CropRepository crops;
  @Autowired private TrustScoreRepository trustScores;
  @Autowired private PasswordEncoder passwordEncoder;

  private User trustedFarmer;
  private Farm trustedFarm;
  private Crop tomato;

  @BeforeEach
  void setUp() {
    trustedFarmer = newFarmer("trusted@test.local", VerificationStatus.IDENTITY_VERIFIED);
    trustedFarm = newFarm(trustedFarmer, "Nashik", true);
    tomato = new Crop();
    tomato.setName("Tomato");
    tomato = crops.save(tomato);
  }

  @Test
  void trustedFarmerReportGetsHighConfidence() {
    SupplyReportResponse created =
        service.create(
            validRequest(trustedFarm, tomato, 10, 20, LocalDate.now(), LocalDate.now().plusDays(30)),
            trustedFarmer.getEmail());

    assertEquals(ReportStatus.SUBMITTED, created.getStatus());
    assertEquals(TrustLevel.HIGH_CONFIDENCE.name(), created.getTrustLevel());
    assertTrue(created.getTrustScore() >= 70.0);

    TrustScore stored = trustScores.findBySupplyReportId(created.getId()).orElseThrow();
    assertEquals(TrustLevel.HIGH_CONFIDENCE, stored.getLevel());
    assertFalse(stored.isRequiresReview());
  }

  @Test
  void lowConfidenceReportStaysSubmittedForReview() {
    User farmer = newFarmer("newbie@test.local", VerificationStatus.UNVERIFIED);
    Farm farm = newFarm(farmer, "Nashik", false);
    // Profile region differs from farm region; harvest far beyond a sane horizon.
    profiles.findByUserId(farmer.getId()).ifPresent(p -> p.setRegion("Pune"));

    SupplyReportResponse created =
        service.create(
            validRequest(
                farm, tomato, 10, 20, LocalDate.now().plusDays(400), LocalDate.now().plusDays(410)),
            farmer.getEmail());

    assertEquals(ReportStatus.SUBMITTED, created.getStatus());
    assertTrue(
        created.getTrustLevel().equals(TrustLevel.LOW_CONFIDENCE.name())
            || created.getTrustLevel().equals(TrustLevel.REQUIRES_REVIEW.name()));

    TrustScore stored = trustScores.findBySupplyReportId(created.getId()).orElseThrow();
    assertTrue(stored.isRequiresReview());
  }

  @Test
  void reportUpdateChangesConfidence() {
    SupplyReportResponse created =
        service.create(
            validRequest(trustedFarm, tomato, 10, 20, LocalDate.now(), LocalDate.now().plusDays(30)),
            trustedFarmer.getEmail());

    UpdateSupplyReportRequest update = new UpdateSupplyReportRequest();
    update.setQuantityMinTonnes(10);
    update.setQuantityMaxTonnes(20);
    update.setHarvestStart(LocalDate.now().plusDays(400));
    update.setHarvestEnd(LocalDate.now().plusDays(410));
    update.setQuality("Good");
    SupplyReportResponse updated =
        service.updateOwn(created.getId(), update, trustedFarmer.getEmail());

    assertTrue(updated.getTrustScore() < created.getTrustScore());
  }

  @Test
  void suspiciousQuantityReducesConfidence() {
    service.create(
        validRequest(trustedFarm, tomato, 10, 20, LocalDate.now(), LocalDate.now().plusDays(30)),
        trustedFarmer.getEmail());
    SupplyReportResponse peer =
        service.create(
            validRequest(trustedFarm, tomato, 12, 18, LocalDate.now(), LocalDate.now().plusDays(30)),
            trustedFarmer.getEmail());
    SupplyReportResponse huge =
        service.create(
            validRequest(
                trustedFarm, tomato, 200, 300, LocalDate.now(), LocalDate.now().plusDays(30)),
            trustedFarmer.getEmail());

    assertTrue(huge.getTrustScore() < peer.getTrustScore());
    TrustScore stored = trustScores.findBySupplyReportId(huge.getId()).orElseThrow();
    assertTrue(stored.isRequiresReview());
  }

  @Test
  void privateReportRemainsPrivate() {
    SupplyReportResponse created =
        service.create(
            validRequest(trustedFarm, tomato, 10, 20, LocalDate.now(), LocalDate.now().plusDays(30)),
            trustedFarmer.getEmail());

    User other = newFarmer("other@test.local", VerificationStatus.IDENTITY_VERIFIED);
    assertThrows(
        AccessDeniedException.class, () -> service.getOne(created.getId(), other.getEmail()));

    List<SupplyReportResponse> othersMine = service.listMine(other.getEmail());
    assertTrue(othersMine.stream().noneMatch(r -> r.getId().equals(created.getId())));
  }

  private User newFarmer(String email, VerificationStatus status) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode("password123"));
    user.setRole(Role.FARMER);
    user.setAccountStatus(AccountStatus.ACTIVE);
    user.setVerificationStatus(status);
    return users.save(user);
  }

  private Farm newFarm(User farmer, String region, boolean fpoValidated) {
    FarmerProfile profile = new FarmerProfile();
    profile.setUser(farmer);
    profile.setFullName("Farmer " + farmer.getEmail());
    profile.setRegion(region);
    profile.setFpoValidated(fpoValidated);
    profile = profiles.save(profile);
    Farm farm = new Farm();
    farm.setFarmer(profile);
    farm.setName("Farm " + farmer.getEmail());
    farm.setRegion(region);
    return farms.save(farm);
  }

  private CreateSupplyReportRequest validRequest(
      Farm farm, Crop crop, double min, double max, LocalDate start, LocalDate end) {
    CreateSupplyReportRequest request = new CreateSupplyReportRequest();
    request.setFarmId(farm.getId());
    request.setCropId(crop.getId());
    request.setQuantityMinTonnes(min);
    request.setQuantityMaxTonnes(max);
    request.setHarvestStart(start);
    request.setHarvestEnd(end);
    request.setQuality("Good");
    request.setRegion("Nashik");
    return request;
  }
}
