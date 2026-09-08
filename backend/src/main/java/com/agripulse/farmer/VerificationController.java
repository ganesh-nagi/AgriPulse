package com.agripulse.farmer;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Farmer verification workflow. Owner-scoped; role comes from the JWT. */
@RestController
@RequestMapping("/api/verification")
@PreAuthorize("hasRole('FARMER')")
public class VerificationController {

  private final FarmerOnboardingService onboardingService;

  public VerificationController(FarmerOnboardingService onboardingService) {
    this.onboardingService = onboardingService;
  }

  @PostMapping("/profile")
  @ResponseStatus(HttpStatus.CREATED)
  public OnboardingStatusResponse createProfile(
      @Valid @RequestBody CreateFarmerProfileRequest request, Principal principal) {
    return onboardingService.createProfile(request, principal.getName());
  }

  @GetMapping("/status")
  public OnboardingStatusResponse status(Principal principal) {
    return onboardingService.getStatus(principal.getName());
  }

  @PostMapping("/phone/request")
  public PhoneVerificationResponse requestPhone(Principal principal) {
    return onboardingService.requestPhoneVerification(principal.getName());
  }

  @PostMapping("/phone/confirm")
  public OnboardingStatusResponse confirmPhone(
      @Valid @RequestBody PhoneCodeRequest request, Principal principal) {
    return onboardingService.confirmPhone(request, principal.getName());
  }

  @PostMapping("/identity/request")
  public OnboardingStatusResponse requestIdentity(Principal principal) {
    return onboardingService.requestIdentityVerification(principal.getName());
  }

  @PostMapping("/identity/confirm")
  public OnboardingStatusResponse confirmIdentity(Principal principal) {
    return onboardingService.confirmIdentity(principal.getName());
  }

  @PostMapping("/farm/{farmId}")
  public OnboardingStatusResponse verifyFarm(
      @PathVariable Long farmId, Principal principal) {
    return onboardingService.verifyFarmRegion(farmId, principal.getName());
  }
}
