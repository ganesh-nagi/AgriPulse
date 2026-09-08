package com.agripulse.user;

import com.agripulse.audit.AuditService;
import com.agripulse.auth.AuthService;
import com.agripulse.demand.BuyerRequirementRepository;
import com.agripulse.demand.RequirementStatus;
import com.agripulse.storage.StorageFacilityRepository;
import com.agripulse.supply.ReportStatus;
import com.agripulse.supply.SupplyReport;
import com.agripulse.supply.SupplyReportRepository;
import com.agripulse.transport.TransportResourceRepository;
import com.agripulse.trust.TrustScore;
import com.agripulse.trust.TrustScoreRepository;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin operations: aggregate analytics plus account role/status management. */
@Service
public class AdminService {

  private final UserRepository users;
  private final AuthService authService;
  private final SupplyReportRepository reports;
  private final TrustScoreRepository trustScores;
  private final BuyerRequirementRepository requirements;
  private final StorageFacilityRepository facilities;
  private final TransportResourceRepository transport;
  private final AuditService auditService;

  public AdminService(
      UserRepository users,
      AuthService authService,
      SupplyReportRepository reports,
      TrustScoreRepository trustScores,
      BuyerRequirementRepository requirements,
      StorageFacilityRepository facilities,
      TransportResourceRepository transport,
      AuditService auditService) {
    this.users = users;
    this.authService = authService;
    this.reports = reports;
    this.trustScores = trustScores;
    this.requirements = requirements;
    this.facilities = facilities;
    this.transport = transport;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public AdminOverviewResponse overview() {
    Map<String, Long> usersByRole =
        users.findAll().stream()
            .collect(Collectors.groupingBy(u -> u.getRole().name(), TreeMap::new, Collectors.counting()));
    Map<String, Long> reportsByStatus = new TreeMap<>();
    for (ReportStatus status : ReportStatus.values()) {
      reportsByStatus.put(status.name(), 0L);
    }
    for (SupplyReport report : reports.findAll()) {
      reportsByStatus.merge(report.getStatus().name(), 1L, Long::sum);
    }
    double averageTrust =
        trustScores.findAll().stream()
            .mapToDouble(TrustScore::getScore)
            .average()
            .orElse(0.0);
    long openRequirements =
        requirements.findAll().stream()
            .filter(r -> r.getStatus() == RequirementStatus.OPEN)
            .count();
    return new AdminOverviewResponse(
        users.count(),
        usersByRole,
        reportsByStatus,
        averageTrust,
        openRequirements,
        facilities.count(),
        transport.count());
  }

  @Transactional
  public void changeRole(Long userId, Role newRole, String adminEmail) {
    authService.changeRole(userId, newRole);
    auditService.record(
        actorIdOf(adminEmail), "ADMIN_ROLE_CHANGED", "User", String.valueOf(userId),
        "role=" + newRole.name());
  }

  @Transactional
  public void changeStatus(Long userId, AccountStatus newStatus, String adminEmail) {
    User user =
        users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    AccountStatus old = user.getAccountStatus();
    user.setAccountStatus(newStatus);
    if (newStatus == AccountStatus.ACTIVE) {
      user.setFailedLoginAttempts(0);
      user.setLockedUntil(null);
    }
    auditService.record(
        actorIdOf(adminEmail),
        "ACCOUNT_STATUS_CHANGED",
        "User",
        String.valueOf(user.getId()),
        "status=" + old.name() + "->" + newStatus.name());
  }

  private Long actorIdOf(String adminEmail) {
    return users.findByEmail(adminEmail).map(User::getId).orElse(null);
  }
}
