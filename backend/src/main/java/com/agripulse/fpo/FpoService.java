package com.agripulse.fpo;

import com.agripulse.audit.AuditService;
import com.agripulse.auth.AccountLockedException;
import com.agripulse.farmer.FarmerProfile;
import com.agripulse.farmer.FarmerProfileRepository;
import com.agripulse.supply.SupplyReportRepository;
import com.agripulse.supply.SupplyReportService;
import com.agripulse.trust.VerificationDecision;
import com.agripulse.trust.VerificationRecord;
import com.agripulse.trust.VerificationRecordRepository;
import com.agripulse.trust.VerifierType;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.Role;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import com.agripulse.user.VerificationStatus;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FPO validation of member farmers. FPOs see permitted member state only
 * (no phones, no coordinates); approving a farmer boosts their reports'
 * trust scores through a re-score.
 */
@Service
public class FpoService {

  private final UserRepository users;
  private final FpoProfileRepository fpoProfiles;
  private final FarmerProfileRepository farmerProfiles;
  private final SupplyReportRepository supplyReports;
  private final SupplyReportService supplyReportService;
  private final VerificationRecordRepository verifications;
  private final AuditService auditService;

  public FpoService(
      UserRepository users,
      FpoProfileRepository fpoProfiles,
      FarmerProfileRepository farmerProfiles,
      SupplyReportRepository supplyReports,
      SupplyReportService supplyReportService,
      VerificationRecordRepository verifications,
      AuditService auditService) {
    this.users = users;
    this.fpoProfiles = fpoProfiles;
    this.farmerProfiles = farmerProfiles;
    this.supplyReports = supplyReports;
    this.supplyReportService = supplyReportService;
    this.verifications = verifications;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<FpoMemberView> membersInRegion(String region, String fpoEmail) {
    fpoOf(fpoEmail);
    return farmerProfiles.findAll().stream()
        .filter(p -> p.getRegion() != null && p.getRegion().equalsIgnoreCase(region.trim()))
        .map(
            p ->
                new FpoMemberView(
                    p.getUser().getId(),
                    p.getFullName(),
                    p.getRegion(),
                    p.getUser().getVerificationStatus(),
                    p.getOnboardingState(),
                    p.isFpoValidated(),
                    supplyReports.findByReporterId(p.getUser().getId()).size()))
        .toList();
  }

  @Transactional
  public void validateFarmer(ValidateFarmerRequest request, String fpoEmail) {
    User fpo = fpoOf(fpoEmail);
    User farmer =
        users
            .findById(request.getFarmerUserId())
            .orElseThrow(() -> new IllegalArgumentException("Farmer not found"));
    if (farmer.getRole() != Role.FARMER) {
      throw new IllegalArgumentException("User is not a farmer");
    }
    FarmerProfile profile =
        farmerProfiles
            .findByUserId(farmer.getId())
            .orElseThrow(() -> new IllegalArgumentException("Farmer profile not found"));
    profile.setFpoValidated(request.isApproved());
    if (request.isApproved()) {
      farmer.setVerificationStatus(VerificationStatus.FPO_VALIDATED);
    }
    VerificationRecord record = new VerificationRecord();
    record.setSubject(farmer);
    record.setVerifierType(VerifierType.FPO);
    record.setMethod("FPO_MEMBER_VALIDATION");
    record.setDecision(
        request.isApproved() ? VerificationDecision.APPROVED : VerificationDecision.REJECTED);
    record.setNotes(request.getNotes());
    verifications.save(record);
    supplyReportService.rescoreReporterReports(farmer.getId());
    auditService.record(
        fpo.getId(),
        "FPO_VALIDATION",
        "User",
        String.valueOf(farmer.getId()),
        "approved=" + request.isApproved());
  }

  private User fpoOf(String email) {
    User user =
        users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (user.getRole() != Role.FPO) {
      throw new AccessDeniedException("FPO role required");
    }
    if (user.getAccountStatus() != AccountStatus.ACTIVE) {
      throw new AccountLockedException("Account is not active");
    }
    fpoProfiles
        .findByUserId(user.getId())
        .orElseGet(
            () -> {
              FpoProfile profile = new FpoProfile();
              profile.setUser(user);
              profile.setFpoName(user.getEmail().split("@")[0]);
              profile.setRegion("UNASSIGNED");
              return fpoProfiles.save(profile);
            });
    return user;
  }
}
