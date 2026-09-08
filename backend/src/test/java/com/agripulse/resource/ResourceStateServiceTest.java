package com.agripulse.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.processing.ProcessingService;
import com.agripulse.storage.CreateStorageRequest;
import com.agripulse.storage.StorageFacilityRepository;
import com.agripulse.storage.StorageFacilityService;
import com.agripulse.storage.StorageResponse;
import com.agripulse.storage.UpdateOccupancyRequest;
import com.agripulse.transport.CreateTransportRequest;
import com.agripulse.transport.TransportResourceService;
import com.agripulse.transport.TransportStatus;
import com.agripulse.transport.UpdateTransportStatusRequest;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
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
class ResourceStateServiceTest {

  @Autowired private ResourceStateService state;
  @Autowired private StorageFacilityService storageService;
  @Autowired private StorageFacilityRepository storageRepo;
  @Autowired private TransportResourceService transportService;
  @Autowired private ProcessingService processingService;
  @Autowired private CropRepository crops;
  @Autowired private UserRepository users;
  @Autowired private PasswordEncoder passwordEncoder;

  private User operator;
  private User transporter;
  private Crop tomato;
  private Crop wheat;

  @BeforeEach
  void setUp() {
    operator = newUser("op@test.local", Role.STORAGE_OPERATOR);
    transporter = newUser("tr@test.local", Role.TRANSPORTER);
    tomato = newCrop("Tomato");
    wheat = newCrop("Wheat");
  }

  @Test
  void normalStorageIsCounted() {
    createStorage(operator, "Cold Store", 100, "Tomato", null, null);

    ResourceSnapshot snap = state.snapshot("Nashik", tomato.getId(), "Tomato", LocalDate.now());

    assertEquals(100.0, snap.getStorageAvailableTonnes());
  }

  @Test
  void zeroStorageIsValid() {
    StorageResponse created = createStorage(operator, "Empty Shed", 0, "Tomato", null, null);

    ResourceSnapshot snap = state.snapshot("Nashik", tomato.getId(), "Tomato", LocalDate.now());

    assertEquals(0.0, created.getAvailableTonnes());
    assertEquals(0.0, snap.getStorageAvailableTonnes());
  }

  @Test
  void invalidCapacityRejected() {
    CreateStorageRequest bad = storageRequest("Bad Store", -5);
    assertThrows(
        IllegalArgumentException.class, () -> storageService.create(bad, operator.getEmail()));

    CreateTransportRequest badTransport = transportRequest(-5);
    assertThrows(
        IllegalArgumentException.class,
        () -> transportService.create(badTransport, transporter.getEmail()));
  }

  @Test
  void occupiedAboveTotalRejected() {
    StorageResponse created = createStorage(operator, "Store", 100, "Tomato", null, null);

    UpdateOccupancyRequest over = new UpdateOccupancyRequest();
    over.setOccupiedTonnes(101);
    assertThrows(
        IllegalArgumentException.class,
        () -> storageService.updateOccupancy(created.getId(), over, operator.getEmail()));

    UpdateOccupancyRequest negative = new UpdateOccupancyRequest();
    negative.setOccupiedTonnes(-1);
    assertThrows(
        IllegalArgumentException.class,
        () -> storageService.updateOccupancy(created.getId(), negative, operator.getEmail()));
  }

  @Test
  void incompatibleCropExcluded() {
    createStorage(operator, "Wheat Only", 100, "Wheat", null, null);

    ResourceSnapshot snap = state.snapshot("Nashik", tomato.getId(), "Tomato", LocalDate.now());

    assertEquals(0.0, snap.getStorageAvailableTonnes());
  }

  @Test
  void transportUnavailableExcluded() {
    var created = transportService.create(transportRequest(20), transporter.getEmail());
    UpdateTransportStatusRequest down = new UpdateTransportStatusRequest();
    down.setStatus(TransportStatus.MAINTENANCE);
    transportService.updateStatus(created.getId(), down, transporter.getEmail());

    // Outside its availability window.
    CreateTransportRequest seasonal = transportRequest(15);
    seasonal.setAvailableFrom(LocalDate.now().minusDays(60));
    seasonal.setAvailableTo(LocalDate.now().minusDays(30));
    transportService.create(seasonal, transporter.getEmail());

    ResourceSnapshot snap = state.snapshot("Nashik", tomato.getId(), "Tomato", LocalDate.now());

    assertEquals(0.0, snap.getTransportAvailableTonnes());
  }

  @Test
  void zeroTransportIsValid() {
    transportService.create(transportRequest(0), transporter.getEmail());

    ResourceSnapshot snap = state.snapshot("Nashik", tomato.getId(), "Tomato", LocalDate.now());

    assertEquals(0.0, snap.getTransportAvailableTonnes());
  }

  @Test
  void processingUnavailableIsZero() {
    ResourceSnapshot empty = state.snapshot("Nashik", tomato.getId(), "Tomato", LocalDate.now());
    assertEquals(0.0, empty.getProcessingAvailableTonnes());

    // Wrong crop and stale window are both excluded.
    processingService.register(
        "Wheat Unit", "Nashik", wheat.getId(), 50, 0, null, null);
    processingService.register(
        "Old Tomato Unit",
        "Nashik",
        tomato.getId(),
        50,
        0,
        LocalDate.now().minusDays(60),
        LocalDate.now().minusDays(30));

    ResourceSnapshot snap = state.snapshot("Nashik", tomato.getId(), "Tomato", LocalDate.now());
    assertEquals(0.0, snap.getProcessingAvailableTonnes());

    // Matching crop and live window counts.
    processingService.register(
        "Tomato Unit", "Nashik", tomato.getId(), 40, 10, null, null);
    ResourceSnapshot live = state.snapshot("Nashik", tomato.getId(), "Tomato", LocalDate.now());
    assertEquals(30.0, live.getProcessingAvailableTonnes());
  }

  private User newUser(String email, Role role) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode("password123"));
    user.setRole(role);
    user.setAccountStatus(AccountStatus.ACTIVE);
    return users.save(user);
  }

  private Crop newCrop(String name) {
    Crop crop = new Crop();
    crop.setName(name);
    return crops.save(crop);
  }

  private CreateStorageRequest storageRequest(String name, double capacity) {
    CreateStorageRequest request = new CreateStorageRequest();
    request.setName(name);
    request.setRegion("Nashik");
    request.setCapacityTonnes(capacity);
    request.setCropCompatibility("Tomato");
    return request;
  }

  private StorageResponse createStorage(
      User op, String name, double capacity, String compatibility, LocalDate from, LocalDate to) {
    CreateStorageRequest request = storageRequest(name, capacity);
    request.setCropCompatibility(compatibility);
    request.setAvailableFrom(from);
    request.setAvailableTo(to);
    return storageService.create(request, op.getEmail());
  }

  private CreateTransportRequest transportRequest(double capacity) {
    CreateTransportRequest request = new CreateTransportRequest();
    request.setCapacityTonnes(capacity);
    request.setOriginRegion("Nashik");
    request.setDestRegion("Mumbai");
    return request;
  }
}
