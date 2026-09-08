package com.agripulse.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.farmer.CreateFarmerProfileRequest;
import com.agripulse.farmer.Farm;
import com.agripulse.farmer.FarmRepository;
import com.agripulse.farmer.FarmerOnboardingService;
import com.agripulse.farmer.FarmerProfile;
import com.agripulse.farmer.FarmerProfileRepository;
import com.agripulse.trust.TrustScore;
import com.agripulse.trust.TrustScoreRepository;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthorizationTest {

  @Autowired private MockMvc mvc;
  @Autowired private AuthService authService;
  @Autowired private JwtService jwtService;
  @Autowired private UserRepository users;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private CropRepository crops;
  @Autowired private FarmerProfileRepository profiles;
  @Autowired private FarmRepository farms;
  @Autowired private FarmerOnboardingService onboardingService;
  @Autowired private TrustScoreRepository trustScores;

  private String farmerA;
  private String farmerB;
  private String buyer;
  private String fpo;
  private String admin;
  private Long cropId;
  private Long farmAId;
  private Long reportAId;

  @BeforeEach
  void setUp() throws Exception {
    farmerA = tokenFor("fa@test.local", Role.FARMER);
    farmerB = tokenFor("fb@test.local", Role.FARMER);
    buyer = tokenFor("buy@test.local", Role.BUYER);
    fpo = tokenFor("fpo@test.local", Role.FPO);
    admin = createAdmin();

    Crop crop = new Crop();
    crop.setName("Tomato");
    cropId = crops.save(crop).getId();

    createProfile("fa@test.local", "Farmer A");
    farmAId = addFarm("fa@test.local");

    String body =
        "{\"farmId\":" + farmAId + ",\"cropId\":" + cropId
            + ",\"quantityMinTonnes\":10,\"quantityMaxTonnes\":20,"
            + "\"harvestStart\":\"2026-09-01\",\"harvestEnd\":\"2026-09-10\","
            + "\"region\":\"Nashik\"}";
    String response =
        mvc.perform(
                post("/api/reports")
                    .header("Authorization", "Bearer " + farmerA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    reportAId = Long.parseLong(response.replaceAll(".*\"id\":(\\d+).*", "$1"));
  }

  @Test
  void unauthenticatedRequestsAreRejected() throws Exception {
    mvc.perform(get("/api/reports/mine")).andExpect(status().is4xxClientError());
  }

  @Test
  void invalidTokenIsRejected() throws Exception {
    mvc.perform(get("/api/reports/mine").header("Authorization", "Bearer invalid.token.here"))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void farmerCannotReadAnotherFarmersReport() throws Exception {
    mvc.perform(get("/api/reports/" + reportAId).header("Authorization", "Bearer " + farmerB))
        .andExpect(status().isForbidden());
  }

  @Test
  void farmerCanReadOwnReport() throws Exception {
    mvc.perform(get("/api/reports/" + reportAId).header("Authorization", "Bearer " + farmerA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.region").value("Nashik"));
  }

  @Test
  void wrongRoleCannotCreateReports() throws Exception {
    String body =
        "{\"farmId\":" + farmAId + ",\"cropId\":" + cropId
            + ",\"quantityMinTonnes\":1,\"quantityMaxTonnes\":2,"
            + "\"harvestStart\":\"2026-09-01\",\"harvestEnd\":\"2026-09-02\",\"region\":\"Nashik\"}";
    mvc.perform(
            post("/api/reports")
                .header("Authorization", "Bearer " + buyer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void farmerCannotPostBuyerRequirements() throws Exception {
    String body =
        "{\"cropId\":" + cropId + ",\"quantityTonnes\":5,"
            + "\"requiredDate\":\"2026-09-15\",\"region\":\"Nashik\"}";
    mvc.perform(
            post("/api/buyer/requirements")
                .header("Authorization", "Bearer " + farmerA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void buyerRequirementsAreOwnerScoped() throws Exception {
    String body =
        "{\"cropId\":" + cropId + ",\"quantityTonnes\":5,"
            + "\"requiredDate\":\"2026-09-15\",\"region\":\"Nashik\"}";
    mvc.perform(
            post("/api/buyer/requirements")
                .header("Authorization", "Bearer " + buyer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    String otherBuyer = tokenFor("buy2@test.local", Role.BUYER);
    mvc.perform(get("/api/buyer/requirements/mine").header("Authorization", "Bearer " + otherBuyer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void fpoValidationBoostsTrustWithoutLeakingPrivateData() throws Exception {
    mvc.perform(get("/api/fpo/members").param("region", "Nashik")
            .header("Authorization", "Bearer " + fpo))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].farmerName").value("Farmer A"))
        .andExpect(jsonPath("$[0].phone").doesNotExist())
        .andExpect(jsonPath("$[0].fpoValidated").value(false));

    Long farmerAId = users.findByEmail("fa@test.local").orElseThrow().getId();
    mvc.perform(
            post("/api/fpo/validations")
                .header("Authorization", "Bearer " + fpo)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"farmerUserId\":" + farmerAId + ",\"approved\":true}"))
        .andExpect(status().isCreated());

    // 20 base + 10 phone + 20 identity + 25 FPO + 10 region-consistent
    TrustScore score = trustScores.findBySupplyReportId(reportAId).orElseThrow();
    assertEquals(85.0, score.getScore());
    assertTrue(score.getSignals().contains("fpo_validated"));
    assertTrue(score.getSignals().contains("region_consistent"));
  }

  @Test
  void adminOverviewIsAdminOnly() throws Exception {
    mvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + farmerA))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUsers").isNumber())
        .andExpect(jsonPath("$.averageTrustScore").isNumber());
  }

  @Test
  void ownerCanPatchOwnReport() throws Exception {
    String body =
        "{\"quantityMinTonnes\":12,\"quantityMaxTonnes\":22,"
            + "\"harvestStart\":\"2026-09-01\",\"harvestEnd\":\"2026-09-12\",\"quality\":\"A\"}";
    mvc.perform(
            patch("/api/reports/" + reportAId)
                .header("Authorization", "Bearer " + farmerA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantityMaxTonnes").value(22.0));
  }

  @Test
  void loginRateLimitTripsAfterBudget() throws Exception {
    String body = "{\"email\":\"nobody@test.local\",\"password\":\"wrong-password1\"}";
    for (int i = 0; i < 20; i++) {
      mvc.perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().is4xxClientError());
    }
    mvc.perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isTooManyRequests());
  }

  private String tokenFor(String email, Role role) {
    RegisterRequest register = new RegisterRequest();
    register.setEmail(email);
    register.setPassword("password123");
    register.setRole(role);
    authService.register(register);
    User user = users.findByEmail(email).orElseThrow();
    return jwtService.generateToken(user.getId(), email, role.name());
  }

  private String createAdmin() {
    User adminUser = new User();
    adminUser.setEmail("admin@test.local");
    adminUser.setPasswordHash(passwordEncoder.encode("password123"));
    adminUser.setRole(Role.ADMIN);
    adminUser.setAccountStatus(AccountStatus.ACTIVE);
    users.save(adminUser);
    return jwtService.generateToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");
  }

  private void createProfile(String email, String name) {
    CreateFarmerProfileRequest profile = new CreateFarmerProfileRequest();
    profile.setFullName(name);
    profile.setPhone("+910000000001");
    profile.setRegion("Nashik");
    onboardingService.createProfile(profile, email);
  }

  private Long addFarm(String email) {
    FarmerProfile profile =
        profiles.findAll().stream()
            .filter(p -> p.getUser().getEmail().equals(email))
            .findFirst()
            .orElseThrow();
    Farm farm = new Farm();
    farm.setFarmer(profile);
    farm.setName("A Farm");
    farm.setRegion("Nashik");
    return farms.save(farm).getId();
  }
}
