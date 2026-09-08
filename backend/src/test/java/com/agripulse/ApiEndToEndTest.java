package com.agripulse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full user journey over real HTTP against a booted server (H2): register,
 * login, lockout, refresh rotation, farmer onboarding, farm + report,
 * cross-farmer isolation, buyer flow, logout invalidation, public aggregates.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiEndToEndTest {

  @LocalServerPort private int port;
  @Autowired private TestRestTemplate rest;
  @Autowired private CropRepository crops;
  @Autowired private UserRepository users;
  @Autowired private PasswordEncoder passwordEncoder;

  private String base;

  @BeforeEach
  void setUp() {
    base = "http://localhost:" + port;
    if (crops.findByName("Tomato").isEmpty()) {
      Crop crop = new Crop();
      crop.setName("Tomato");
      crops.save(crop);
    }
  }

  @Test
  void fullJourney() {
    assertEquals("UP", get("/api/public/health", null, Map.class).get("status"));

    // Register farmer + buyer.
    Map<String, Object> farmerAuth =
        post("/api/auth/register",
            Map.of("email", "e2e@test.local", "password", "Farmer1234", "role", "FARMER"), null);
    String farmerToken = (String) farmerAuth.get("token");
    String farmerRefresh = (String) farmerAuth.get("refreshToken");
    assertNotNull(farmerToken);

    Map<String, Object> buyerAuth =
        post("/api/auth/register",
            Map.of("email", "e2ebuy@test.local", "password", "Buyer1234", "role", "BUYER"), null);
    String buyerToken = (String) buyerAuth.get("token");

    // Wrong password is rejected.
    assertStatus(HttpStatus.BAD_REQUEST,
        rawPost("/api/auth/login", Map.of("email", "e2e@test.local", "password", "Wrong1234")));

    // Lockout after repeated failures on a dedicated account.
    post("/api/auth/register",
        Map.of("email", "e2elock@test.local", "password", "Lock1234", "role", "FARMER"), null);
    for (int i = 0; i < 5; i++) {
      assertStatus(HttpStatus.BAD_REQUEST,
          rawPost("/api/auth/login", Map.of("email", "e2elock@test.local", "password", "Wrong1234")));
    }
    assertStatus(HttpStatus.LOCKED,
        rawPost("/api/auth/login", Map.of("email", "e2elock@test.local", "password", "Lock1234")));

    // Refresh rotation; reuse is rejected.
    Map<String, Object> rotated =
        post("/api/auth/refresh", Map.of("refreshToken", farmerRefresh), null);
    farmerToken = (String) rotated.get("token");
    String rotatedRefresh = (String) rotated.get("refreshToken");
    assertStatus(HttpStatus.BAD_REQUEST,
        rawPost("/api/auth/refresh", Map.of("refreshToken", farmerRefresh)));

    // Onboarding: profile → phone → identity.
    post("/api/verification/profile",
        Map.of("fullName", "E2E Farmer", "phone", "+910000000001", "region", "Nashik"), farmerToken);
    Map<String, Object> phone =
        post("/api/verification/phone/request", Map.of(), farmerToken);
    assertStatus(HttpStatus.BAD_REQUEST,
        rawPost("/api/verification/phone/confirm", Map.of("code", "000000"), farmerToken));
    Map<String, Object> phoneOk =
        post("/api/verification/phone/confirm", Map.of("code", phone.get("demoCode")), farmerToken);
    assertEquals("PHONE_VERIFIED", phoneOk.get("state"));

    post("/api/verification/identity/request", Map.of(), farmerToken);
    Map<String, Object> identityOk =
        post("/api/verification/identity/confirm", Map.of(), farmerToken);
    assertEquals("IDENTITY_VERIFIED", identityOk.get("state"));

    // Farm + region activation.
    Map<String, Object> farm =
        post("/api/farmer/farms",
            Map.of("name", "E2E Farm", "region", "Nashik", "areaAcres", 2.5), farmerToken);
    Number farmId = (Number) farm.get("id");
    Map<String, Object> active =
        post("/api/verification/farm/" + farmId.longValue(), Map.of(), farmerToken);
    assertEquals("PROFILE_ACTIVE", active.get("state"));

    // Supply report by owner.
    Long cropId = crops.findByName("Tomato").orElseThrow().getId();
    Map<String, Object> report =
        post("/api/reports",
            Map.of("farmId", farmId.longValue(), "cropId", cropId,
                "quantityMinTonnes", 10, "quantityMaxTonnes", 20,
                "harvestStart", "2026-09-01", "harvestEnd", "2026-09-10",
                "region", "Nashik"),
            farmerToken);
    Number reportId = (Number) report.get("id");
    assertTrue(((Number) report.get("trustScore")).doubleValue() > 0);

    // Second farmer cannot read it; invalid token is rejected.
    Map<String, Object> farmer2 =
        post("/api/auth/register",
            Map.of("email", "e2e2@test.local", "password", "Farmer1234", "role", "FARMER"), null);
    assertStatus(HttpStatus.FORBIDDEN,
        rawGet("/api/reports/" + reportId.longValue(), (String) farmer2.get("token")));
    assertStatus4xx(rawGet("/api/reports/" + reportId.longValue(), "invalid.token.here"));

    // Buyer flow is isolated to the buyer.
    post("/api/buyer/requirements",
        Map.of("cropId", cropId, "quantityTonnes", 40,
            "requiredDate", "2026-09-20", "region", "Nashik"),
        buyerToken);
    assertEquals(1,
        ((java.util.List<?>) get("/api/buyer/requirements/mine", buyerToken, java.util.List.class))
            .size());

    // Admin overview works for admins only.
    User adminUser = new User();
    adminUser.setEmail("e2eadm@test.local");
    adminUser.setPasswordHash(passwordEncoder.encode("Admin1234"));
    adminUser.setRole(Role.ADMIN);
    adminUser.setAccountStatus(AccountStatus.ACTIVE);
    users.save(adminUser);
    Map<String, Object> adminLogin =
        post("/api/auth/login",
            Map.of("email", "e2eadm@test.local", "password", "Admin1234"), null);
    Map<String, Object> overview =
        get("/api/admin/overview", (String) adminLogin.get("token"), Map.class);
    assertTrue(((Number) overview.get("totalUsers")).longValue() >= 4);
    assertStatus(HttpStatus.FORBIDDEN, rawGet("/api/admin/overview", farmerToken));

    // Public regional aggregate exposes no private data.
    String regional =
        rest.getForObject(base + "/api/public/regional/supply?region=Nashik", String.class);
    assertNotNull(regional);
    assertTrue(!regional.contains("E2E Farmer"));

    // Logout invalidates both tokens.
    assertStatus(HttpStatus.NO_CONTENT, rawPost("/api/auth/logout", Map.of(), farmerToken));
    assertStatus4xx(rawGet("/api/reports/mine", farmerToken));
    assertStatus(HttpStatus.BAD_REQUEST,
        rawPost("/api/auth/refresh", Map.of("refreshToken", rotatedRefresh)));

    // Weak passwords never reach the database.
    assertStatus(HttpStatus.BAD_REQUEST,
        rawPost("/api/auth/register",
            Map.of("email", "e2eweak@test.local", "password", "short", "role", "FARMER")));
  }

  private Map<String, Object> post(String path, Object body, String token) {
    ResponseEntity<Map> response = rawPost(path, body, token);
    assertTrue(response.getStatusCode().is2xxSuccessful(),
        "POST " + path + " -> " + response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> result = response.getBody();
    assertNotNull(result);
    return result;
  }

  private ResponseEntity<Map> rawPost(String path, Object body) {
    return rawPost(path, body, null);
  }

  private ResponseEntity<Map> rawPost(String path, Object body, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return rest.postForEntity(base + path, new HttpEntity<>(body, headers), Map.class);
  }

  private <T> T get(String path, String token, Class<T> type) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    ResponseEntity<T> response =
        rest.exchange(base + path, HttpMethod.GET, new HttpEntity<>(headers), type);
    assertEquals(HttpStatus.OK, response.getStatusCode(), "GET " + path);
    return response.getBody();
  }

  private ResponseEntity<String> rawGet(String path, String token) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return rest.exchange(base + path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }

  private void assertStatus(HttpStatus expected, ResponseEntity<?> response) {
    assertEquals(expected, response.getStatusCode());
  }

  private void assertStatus4xx(ResponseEntity<?> response) {
    assertTrue(response.getStatusCode().is4xxClientError(),
        "expected 4xx but got " + response.getStatusCode());
  }
}
