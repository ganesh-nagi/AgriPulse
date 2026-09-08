package com.agripulse.supply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agripulse.auth.AccountLockedException;
import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.farmer.Farm;
import com.agripulse.farmer.FarmRepository;
import com.agripulse.farmer.FarmerProfile;
import com.agripulse.farmer.FarmerProfileRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SupplyReportServiceTest {

  @Autowired private SupplyReportService service;
  @Autowired private UserRepository users;
  @Autowired private FarmerProfileRepository profiles;
  @Autowired private FarmRepository farms;
  @Autowired private CropRepository crops;
  @Autowired private PasswordEncoder passwordEncoder;

  private User farmerA;
  private User farmerB;
  private Farm farmA;
  private Crop tomato;

  @BeforeEach
  void setUp() {
    farmerA = newFarmer("sra@test.local");
    farmerB = newFarmer("srb@test.local");
    farmA = newFarm(farmerA, "Nashik");
    tomato = new Crop();
    tomato.setName("Tomato");
    tomato = crops.save(tomato);
  }

  @Test
  void createAndReadOwnReport() {
    SupplyReportResponse created = createReport(farmerA, farmA, tomato, 10, 20);
    assertEquals(ReportStatus.SUBMITTED, created.getStatus());
    assertEquals("Nashik", created.getRegion());

    SupplyReportResponse one = service.getOne(created.getId(), farmerA.getEmail());
    assertEquals(created.getId(), one.getId());

    List<SupplyReportResponse> mine = service.listMine(farmerA.getEmail());
    assertTrue(mine.stream().anyMatch(r -> r.getId().equals(created.getId())));
  }

  @Test
  void crossFarmerReadIsDenied() {
    SupplyReportResponse created = createReport(farmerA, farmA, tomato, 10, 20);
    assertThrows(
        AccessDeniedException.class, () -> service.getOne(created.getId(), farmerB.getEmail()));
  }

  @Test
  void crossFarmerUpdateIsDenied() {
    SupplyReportResponse created = createReport(farmerA, farmA, tomato, 10, 20);
    assertThrows(
        AccessDeniedException.class,
        () -> service.updateOwn(created.getId(), update(10, 20), farmerB.getEmail()));
  }

  @Test
  void createWithOtherFarmersFarmIsDenied() {
    Farm farmB = newFarm(farmerB, "Nashik");
    CreateSupplyReportRequest request = validRequest(farmA, tomato);
    request.setFarmId(farmB.getId());
    assertThrows(
        AccessDeniedException.class, () -> service.create(request, farmerA.getEmail()));
  }

  @Test
  void invalidQuantityMinGreaterThanMax() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.create(validRequest(farmA, tomato, 30, 20), farmerA.getEmail()));
  }

  @Test
  void invalidQuantityMaxZero() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.create(validRequest(farmA, tomato, 0, 0), farmerA.getEmail()));
  }

  @Test
  void invalidHarvestDates() {
    CreateSupplyReportRequest request = validRequest(farmA, tomato);
    request.setHarvestStart(LocalDate.of(2026, 9, 10));
    request.setHarvestEnd(LocalDate.of(2026, 9, 1));
    assertThrows(
        IllegalArgumentException.class, () -> service.create(request, farmerA.getEmail()));
  }

  @Test
  void unknownCropIsRejected() {
    CreateSupplyReportRequest request = validRequest(farmA, tomato);
    request.setCropId(999999L);
    assertThrows(
        IllegalArgumentException.class, () -> service.create(request, farmerA.getEmail()));
  }

  @Test
  void lockedFarmerCannotSubmit() {
    lock(farmerA);
    assertThrows(
        AccountLockedException.class,
        () -> service.create(validRequest(farmA, tomato), farmerA.getEmail()));
  }

  @Test
  void lockedFarmerCannotUpdate() {
    SupplyReportResponse created = createReport(farmerA, farmA, tomato, 10, 20);
    lock(farmerA);
    assertThrows(
        AccountLockedException.class,
        () -> service.updateOwn(created.getId(), update(10, 20), farmerA.getEmail()));
  }

  @Test
  void cancelOwnReport() {
    SupplyReportResponse created = createReport(farmerA, farmA, tomato, 10, 20);
    SupplyReportResponse cancelled = service.cancelOwn(created.getId(), farmerA.getEmail());
    assertEquals(ReportStatus.CANCELLED, cancelled.getStatus());
    assertThrows(
        IllegalArgumentException.class,
        () -> service.cancelOwn(created.getId(), farmerA.getEmail()));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.updateOwn(created.getId(), update(10, 20), farmerA.getEmail()));
  }

  @Test
  void crossFarmerCancelIsDenied() {
    SupplyReportResponse created = createReport(farmerA, farmA, tomato, 10, 20);
    assertThrows(
        AccessDeniedException.class,
        () -> service.cancelOwn(created.getId(), farmerB.getEmail()));
  }

  private User newFarmer(String email) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode("password123"));
    user.setRole(Role.FARMER);
    user.setAccountStatus(AccountStatus.ACTIVE);
    return users.save(user);
  }

  private Farm newFarm(User farmer, String region) {
    FarmerProfile profile = new FarmerProfile();
    profile.setUser(farmer);
    profile.setFullName("Farmer " + farmer.getEmail());
    profile.setRegion(region);
    profile = profiles.save(profile);
    Farm farm = new Farm();
    farm.setFarmer(profile);
    farm.setName("Farm " + farmer.getEmail());
    farm.setRegion(region);
    return farms.save(farm);
  }

  private void lock(User farmer) {
    farmer.setAccountStatus(AccountStatus.LOCKED);
    users.save(farmer);
  }

  private SupplyReportResponse createReport(
      User farmer, Farm farm, Crop crop, double min, double max) {
    return service.create(validRequest(farm, crop, min, max), farmer.getEmail());
  }

  private CreateSupplyReportRequest validRequest(Farm farm, Crop crop) {
    return validRequest(farm, crop, 10, 20);
  }

  private CreateSupplyReportRequest validRequest(Farm farm, Crop crop, double min, double max) {
    CreateSupplyReportRequest request = new CreateSupplyReportRequest();
    request.setFarmId(farm.getId());
    request.setCropId(crop.getId());
    request.setQuantityMinTonnes(min);
    request.setQuantityMaxTonnes(max);
    request.setHarvestStart(LocalDate.of(2026, 9, 1));
    request.setHarvestEnd(LocalDate.of(2026, 9, 10));
    request.setRegion("Nashik");
    return request;
  }

  private UpdateSupplyReportRequest update(double min, double max) {
    UpdateSupplyReportRequest request = new UpdateSupplyReportRequest();
    request.setQuantityMinTonnes(min);
    request.setQuantityMaxTonnes(max);
    request.setHarvestStart(LocalDate.of(2026, 9, 1));
    request.setHarvestEnd(LocalDate.of(2026, 9, 10));
    return request;
  }
}
