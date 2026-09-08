package com.agripulse.farmer;

import com.agripulse.audit.AuditService;
import com.agripulse.auth.AuthService;
import com.agripulse.trust.VerificationDecision;
import com.agripulse.trust.VerificationProvider;
import com.agripulse.trust.VerificationRecord;
import com.agripulse.trust.VerificationResult;
import com.agripulse.trust.VerifierType;
import com.agripulse.trust.VerificationRecordRepository;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import com.agripulse.user.VerificationStatus;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Farmer verification workflow. Every step is owner-scoped (resolved from the
 * authenticated email, never from client-supplied ids) and audit-logged.
 */
@Service
public class FarmerOnboardingService {

  private final UserRepository users;
  private final FarmerProfileRepository profiles;
  private final FarmRepository farms;
  private final VerificationRecordRepository verifications;
  private final VerificationProvider verificationProvider;
  private final AuditService auditService;

  public FarmerOnboardingService(
      UserRepository users,
      FarmerProfileRepository profiles,
      FarmRepository farms,
      VerificationRecordRepository verifications,
      VerificationProvider verificationProvider,
      AuditService auditService) {
    this.users = users;
    this.profiles = profiles;
    this.farms = farms;
    this.verifications = verifications;
    this.verificationProvider = verificationProvider;
    this.auditService = auditService;
  }

  @Transactional
  public OnboardingStatusResponse createProfile(
      CreateFarmerProfileRequest request, String email) {
    User user = farmerOf(email);
    profiles
        .findByUserId(user.getId())
        .ifPresent(
            p -> {
              throw new IllegalArgumentException("Farmer profile already exists");
            });
    FarmerProfile profile = new FarmerProfile();
    profile.setUser(user);
    profile.setFullName(request.getFullName().trim());
    profile.setPhone(request.getPhone());
    profile.setRegion(request.getRegion().trim());
    profile.setOnboardingState(OnboardingState.ACCOUNT_CREATED);
    profiles.save(profile);
    auditService.record(
        user.getId(), "PROFILE_CREATED", "FarmerProfile", String.valueOf(profile.getId()), null);
    return statusOf(user, profile);
  }

  @Transactional(readOnly = true)
  public OnboardingStatusResponse getStatus(String email) {
    User user = farmerOf(email);
    FarmerProfile profile = requireProfile(user);
    return statusOf(user, profile);
  }

  @Transactional
  public PhoneVerificationResponse requestPhoneVerification(String email) {
    User user = farmerOf(email);
    FarmerProfile profile = requireProfile(user);
    requireState(profile, OnboardingState.ACCOUNT_CREATED, OnboardingState.PHONE_VERIFICATION_PENDING);
    String phone = profile.getPhone() != null ? profile.getPhone() : user.getPhone();
    VerificationResult result = verificationProvider.verifyPhone(user, phone);
    if (result.decision() == VerificationDecision.REJECTED) {
      throw new IllegalArgumentException("Phone number cannot be verified");
    }
    VerificationRecord record = new VerificationRecord();
    record.setSubject(user);
    record.setVerifierType(VerifierType.MOCK_KYC);
    record.setMethod("MOCK_SMS_OTP");
    record.setDecision(VerificationDecision.PENDING);
    record.setNotes(result.note() + " ref=" + result.reference());
    record.setSecretHash(AuthService.sha256(result.channelCode()));
    verifications.save(record);
    profile.setOnboardingState(OnboardingState.PHONE_VERIFICATION_PENDING);
    auditService.record(
        user.getId(), "VERIFICATION_PHONE_REQUESTED", "User", String.valueOf(user.getId()), null);
    return new PhoneVerificationResponse(
        profile.getOnboardingState(), result.channelCode(), result.note());
  }

  @Transactional
  public OnboardingStatusResponse confirmPhone(PhoneCodeRequest request, String email) {
    User user = farmerOf(email);
    FarmerProfile profile = requireProfile(user);
    requireState(profile, OnboardingState.PHONE_VERIFICATION_PENDING);
    VerificationRecord record =
        verifications.findBySubjectId(user.getId()).stream()
            .filter(r -> r.getDecision() == VerificationDecision.PENDING
                && "MOCK_SMS_OTP".equals(r.getMethod()))
            .reduce((first, second) -> second)
            .orElseThrow(() -> new IllegalArgumentException("No pending phone verification"));
    if (!AuthService.sha256(request.getCode().trim()).equals(record.getSecretHash())) {
      auditService.record(
          user.getId(), "VERIFICATION_PHONE_FAILED", "User", String.valueOf(user.getId()), null);
      throw new IllegalArgumentException("Incorrect verification code");
    }
    record.setDecision(VerificationDecision.APPROVED);
    user.setPhoneVerified(true);
    user.setVerificationStatus(VerificationStatus.PHONE_VERIFIED);
    profile.setOnboardingState(OnboardingState.PHONE_VERIFIED);
    auditService.record(
        user.getId(), "VERIFICATION_PHONE", "User", String.valueOf(user.getId()), null);
    return statusOf(user, profile);
  }

  @Transactional
  public OnboardingStatusResponse requestIdentityVerification(String email) {
    User user = farmerOf(email);
    FarmerProfile profile = requireProfile(user);
    requireState(profile, OnboardingState.PHONE_VERIFIED);
    VerificationResult result =
        verificationProvider.verifyIdentity(user, profile.getFullName(), profile.getRegion());
    VerificationRecord record = new VerificationRecord();
    record.setSubject(user);
    record.setVerifierType(VerifierType.MOCK_KYC);
    record.setMethod("MOCK_IDENTITY");
    record.setDecision(VerificationDecision.PENDING);
    record.setNotes(result.note() + " ref=" + result.reference());
    verifications.save(record);
    profile.setOnboardingState(OnboardingState.IDENTITY_VERIFICATION_PENDING);
    auditService.record(
        user.getId(), "VERIFICATION_IDENTITY_REQUESTED", "User", String.valueOf(user.getId()), null);
    return statusOf(user, profile);
  }

  @Transactional
  public OnboardingStatusResponse confirmIdentity(String email) {
    User user = farmerOf(email);
    FarmerProfile profile = requireProfile(user);
    requireState(profile, OnboardingState.IDENTITY_VERIFICATION_PENDING);
    VerificationRecord record =
        verifications.findBySubjectId(user.getId()).stream()
            .filter(r -> r.getDecision() == VerificationDecision.PENDING
                && "MOCK_IDENTITY".equals(r.getMethod()))
            .reduce((first, second) -> second)
            .orElseThrow(() -> new IllegalArgumentException("No pending identity verification"));
    String reference =
        record.getNotes() != null && record.getNotes().contains("ref=")
            ? record.getNotes().substring(record.getNotes().indexOf("ref=") + 4)
            : "";
    VerificationResult result = verificationProvider.confirmIdentity(user, reference);
    if (result.decision() == VerificationDecision.PENDING) {
      profile.setOnboardingState(OnboardingState.REQUIRES_REVIEW);
      auditService.record(
          user.getId(), "VERIFICATION_REVIEW", "User", String.valueOf(user.getId()), null);
      return statusOf(user, profile);
    }
    record.setDecision(VerificationDecision.APPROVED);
    user.setVerificationStatus(VerificationStatus.IDENTITY_VERIFIED);
    profile.setOnboardingState(OnboardingState.IDENTITY_VERIFIED);
    auditService.record(
        user.getId(), "VERIFICATION_IDENTITY", "User", String.valueOf(user.getId()), null);
    return statusOf(user, profile);
  }

  /**
   * Farm/region check: the farm must belong to the farmer and sit in the
   * profile region. Passing it activates the profile.
   */
  @Transactional
  public OnboardingStatusResponse verifyFarmRegion(Long farmId, String email) {
    User user = farmerOf(email);
    FarmerProfile profile = requireProfile(user);
    requireState(profile, OnboardingState.IDENTITY_VERIFIED);
    Farm farm =
        farms.findById(farmId).orElseThrow(() -> new IllegalArgumentException("Farm not found"));
    if (!farm.getFarmer().getId().equals(profile.getId())) {
      throw new AccessDeniedException("Farm does not belong to the farmer");
    }
    boolean consistent =
        farm.getRegion() != null && farm.getRegion().equalsIgnoreCase(profile.getRegion());
    VerificationRecord record = new VerificationRecord();
    record.setSubject(user);
    record.setVerifierType(VerifierType.SELF);
    record.setMethod("REGION_CONSISTENCY");
    record.setDecision(
        consistent ? VerificationDecision.APPROVED : VerificationDecision.REJECTED);
    record.setNotes("farm region=" + farm.getRegion() + " profile region=" + profile.getRegion());
    verifications.save(record);
    if (!consistent) {
      auditService.record(
          user.getId(), "VERIFICATION_REGION_FAILED", "User", String.valueOf(user.getId()), null);
      throw new IllegalArgumentException("Farm region does not match profile region");
    }
    profile.setOnboardingState(OnboardingState.PROFILE_ACTIVE);
    auditService.record(
        user.getId(), "PROFILE_ACTIVATED", "FarmerProfile", String.valueOf(profile.getId()), null);
    return statusOf(user, profile);
  }

  private User farmerOf(String email) {
    User user =
        users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (user.getRole() != Role.FARMER) {
      throw new AccessDeniedException("Farmer role required");
    }
    return user;
  }

  private FarmerProfile requireProfile(User user) {
    return profiles
        .findByUserId(user.getId())
        .orElseThrow(() -> new IllegalArgumentException("Farmer profile not created yet"));
  }

  private void requireState(FarmerProfile profile, OnboardingState... allowed) {
    for (OnboardingState state : allowed) {
      if (profile.getOnboardingState() == state) {
        return;
      }
    }
    throw new IllegalArgumentException(
        "Step not available from state " + profile.getOnboardingState());
  }

  private OnboardingStatusResponse statusOf(User user, FarmerProfile profile) {
    List<Farm> farmerFarms = farms.findByFarmerId(profile.getId());
    Boolean consistent =
        farmerFarms.isEmpty()
            ? null
            : farmerFarms.stream()
                .allMatch(
                    f ->
                        f.getRegion() != null
                            && f.getRegion().equalsIgnoreCase(profile.getRegion()));
    return new OnboardingStatusResponse(
        profile.getOnboardingState(),
        user.isPhoneVerified(),
        user.getVerificationStatus(),
        profile.isFpoValidated(),
        consistent,
        0L);
  }
}
