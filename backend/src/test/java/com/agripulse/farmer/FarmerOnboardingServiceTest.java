package com.agripulse.farmer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agripulse.auth.AuthService;
import com.agripulse.auth.RegisterRequest;
import com.agripulse.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FarmerOnboardingServiceTest {

  @Autowired private AuthService authService;
  @Autowired private FarmerOnboardingService onboardingService;
  @Autowired private FarmerProfileRepository profiles;
  @Autowired private FarmRepository farms;

  private String registerFarmer(String email) {
    RegisterRequest register = new RegisterRequest();
    register.setEmail(email);
    register.setPassword("password123");
    register.setRole(Role.FARMER);
    authService.register(register);
    return email;
  }

  private void createProfile(String email, String name) {
    CreateFarmerProfileRequest profile = new CreateFarmerProfileRequest();
    profile.setFullName(name);
    profile.setPhone("+910000000001");
    profile.setRegion("Nashik");
    onboardingService.createProfile(profile, email);
  }

  private void verifyPhone(String email) {
    PhoneVerificationResponse phone = onboardingService.requestPhoneVerification(email);
    PhoneCodeRequest code = new PhoneCodeRequest();
    code.setCode(phone.getDemoCode());
    onboardingService.confirmPhone(code, email);
  }

  private Farm addFarm(String email, String region) {
    FarmerProfile profile =
        profiles.findAll().stream()
            .filter(p -> p.getUser().getEmail().equals(email))
            .findFirst()
            .orElseThrow();
    Farm farm = new Farm();
    farm.setFarmer(profile);
    farm.setName("Test Farm");
    farm.setRegion(region);
    return farms.save(farm);
  }

  @Test
  void fullHappyPathActivatesProfile() {
    String email = registerFarmer("happy@test.local");
    createProfile(email, "Happy Farmer");
    verifyPhone(email);

    assertEquals(
        OnboardingState.IDENTITY_VERIFICATION_PENDING,
        onboardingService.requestIdentityVerification(email).getState());
    assertEquals(
        OnboardingState.IDENTITY_VERIFIED, onboardingService.confirmIdentity(email).getState());

    Farm farm = addFarm(email, "Nashik");
    assertEquals(
        OnboardingState.PROFILE_ACTIVE,
        onboardingService.verifyFarmRegion(farm.getId(), email).getState());
  }

  @Test
  void reviewNamesGoToManualReview() {
    String email = registerFarmer("review@test.local");
    createProfile(email, "Review Candidate");
    verifyPhone(email);

    onboardingService.requestIdentityVerification(email);
    assertEquals(
        OnboardingState.REQUIRES_REVIEW, onboardingService.confirmIdentity(email).getState());
  }

  @Test
  void wrongCodeIsRejected() {
    String email = registerFarmer("code@test.local");
    createProfile(email, "Code Farmer");
    onboardingService.requestPhoneVerification(email);

    PhoneCodeRequest wrong = new PhoneCodeRequest();
    wrong.setCode("000000");
    assertThrows(IllegalArgumentException.class, () -> onboardingService.confirmPhone(wrong, email));
  }

  @Test
  void regionMismatchBlocksActivation() {
    String email = registerFarmer("mismatch@test.local");
    createProfile(email, "Mismatch Farmer");
    verifyPhone(email);

    onboardingService.requestIdentityVerification(email);
    onboardingService.confirmIdentity(email);

    Farm farm = addFarm(email, "Pune");
    assertThrows(
        IllegalArgumentException.class, () -> onboardingService.verifyFarmRegion(farm.getId(), email));
  }

  @Test
  void stepsEnforceOrder() {
    String email = registerFarmer("order@test.local");
    createProfile(email, "Order Farmer");
    // Identity before phone must fail.
    assertThrows(
        IllegalArgumentException.class, () -> onboardingService.requestIdentityVerification(email));
    assertTrue(
        onboardingService.getStatus(email).getState() == OnboardingState.ACCOUNT_CREATED);
  }
}
