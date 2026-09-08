package com.agripulse.supply;

import com.agripulse.audit.AuditService;
import com.agripulse.auth.AccountLockedException;
import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.farmer.Farm;
import com.agripulse.farmer.FarmRepository;
import com.agripulse.farmer.FarmerProfile;
import com.agripulse.farmer.FarmerProfileRepository;
import com.agripulse.trust.TrustScore;
import com.agripulse.trust.TrustScoreRepository;
import com.agripulse.trust.TrustScoreService;
import com.agripulse.user.AccountStatus;
import com.agripulse.user.User;
import com.agripulse.user.UserRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supply report business rules live here, not in the controller:
 * range/window validation, farm ownership check, initial trust scoring.
 */
@Service
public class SupplyReportService {

  private final SupplyReportRepository reports;
  private final FarmRepository farms;
  private final FarmerProfileRepository farmerProfiles;
  private final SupplyEvidenceRepository evidence;
  private final CropRepository crops;
  private final UserRepository users;
  private final TrustScoreRepository trustScores;
  private final TrustScoreService trustScoreService;
  private final AuditService auditService;

  public SupplyReportService(
      SupplyReportRepository reports,
      FarmRepository farms,
      FarmerProfileRepository farmerProfiles,
      SupplyEvidenceRepository evidence,
      CropRepository crops,
      UserRepository users,
      TrustScoreRepository trustScores,
      TrustScoreService trustScoreService,
      AuditService auditService) {
    this.reports = reports;
    this.farms = farms;
    this.farmerProfiles = farmerProfiles;
    this.evidence = evidence;
    this.crops = crops;
    this.users = users;
    this.trustScores = trustScores;
    this.trustScoreService = trustScoreService;
    this.auditService = auditService;
  }

  @Transactional
  public SupplyReportResponse create(CreateSupplyReportRequest request, String reporterEmail) {
    if (request.getQuantityMinTonnes() > request.getQuantityMaxTonnes()) {
      throw new IllegalArgumentException("quantityMin must be <= quantityMax");
    }
    if (request.getHarvestStart().isAfter(request.getHarvestEnd())) {
      throw new IllegalArgumentException("harvestStart must be on or before harvestEnd");
    }
    User reporter =
        users
            .findByEmail(reporterEmail)
            .orElseThrow(() -> new IllegalArgumentException("Reporter not found"));
    ensureActive(reporter);
    Farm farm =
        farms.findById(request.getFarmId())
            .orElseThrow(() -> new IllegalArgumentException("Farm not found"));
    if (!farm.getFarmer().getUser().getId().equals(reporter.getId())) {
      throw new AccessDeniedException("Farm does not belong to the reporter");
    }
    Crop crop =
        crops.findById(request.getCropId())
            .orElseThrow(() -> new IllegalArgumentException("Crop not found"));

    SupplyReport report = new SupplyReport();
    report.setFarm(farm);
    report.setCrop(crop);
    report.setReporter(reporter);
    report.setQuantityMinTonnes(request.getQuantityMinTonnes());
    report.setQuantityMaxTonnes(request.getQuantityMaxTonnes());
    report.setHarvestStart(request.getHarvestStart());
    report.setHarvestEnd(request.getHarvestEnd());
    report.setQuality(request.getQuality());
    report.setRegion(request.getRegion());
    report.setLatitude(request.getLatitude());
    report.setLongitude(request.getLongitude());
    report.setStatus(ReportStatus.SUBMITTED);
    reports.save(report);

    TrustScoreService.Score score = scoreFor(reporter, report, farm);
    TrustScore trustScore = new TrustScore();
    trustScore.setSupplyReport(report);
    trustScore.setScore(score.value());
    trustScore.setSignals(score.signals());
    trustScores.save(trustScore);

    auditService.record(
        reporter.getId(),
        "SUPPLY_REPORT_CREATED",
        "SupplyReport",
        String.valueOf(report.getId()),
        "region=" + report.getRegion());
    return toResponse(report, score.value());
  }

  @Transactional(readOnly = true)
  public List<SupplyReportResponse> listMine(String reporterEmail) {
    User reporter =
        users
            .findByEmail(reporterEmail)
            .orElseThrow(() -> new IllegalArgumentException("Reporter not found"));
    return reports.findByReporterId(reporter.getId()).stream()
        .map(
            r ->
                toResponse(
                    r,
                    trustScores
                        .findBySupplyReportId(r.getId())
                        .map(TrustScore::getScore)
                        .orElse(0.0)))
        .toList();
  }

  /** Owner-scoped single read. Other farmers can never fetch this. */
  @Transactional(readOnly = true)
  public SupplyReportResponse getOne(Long reportId, String reporterEmail) {
    SupplyReport report = ownedReport(reportId, reporterEmail);
    double score =
        trustScores
            .findBySupplyReportId(report.getId())
            .map(TrustScore::getScore)
            .orElse(0.0);
    return toResponse(report, score);
  }

  /** Owner update of their own DRAFT/SUBMITTED report; re-scored afterwards. */
  @Transactional
  public SupplyReportResponse updateOwn(
      Long reportId, UpdateSupplyReportRequest request, String reporterEmail) {
    SupplyReport report = ownedReport(reportId, reporterEmail);
    if (report.getStatus() != ReportStatus.DRAFT
        && report.getStatus() != ReportStatus.SUBMITTED) {
      throw new IllegalArgumentException("Only draft or submitted reports can be edited");
    }
    if (request.getQuantityMinTonnes() > request.getQuantityMaxTonnes()) {
      throw new IllegalArgumentException("quantityMin must be <= quantityMax");
    }
    if (request.getHarvestStart().isAfter(request.getHarvestEnd())) {
      throw new IllegalArgumentException("harvestStart must be on or before harvestEnd");
    }
    report.setQuantityMinTonnes(request.getQuantityMinTonnes());
    report.setQuantityMaxTonnes(request.getQuantityMaxTonnes());
    report.setHarvestStart(request.getHarvestStart());
    report.setHarvestEnd(request.getHarvestEnd());
    report.setQuality(request.getQuality());
    TrustScoreService.Score score =
        scoreFor(report.getReporter(), report, report.getFarm());
    trustScores
        .findBySupplyReportId(report.getId())
        .ifPresent(
            ts -> {
              ts.setScore(score.value());
              ts.setSignals(score.signals());
            });
    auditService.record(
        report.getReporter().getId(),
        "SUPPLY_REPORT_UPDATED",
        "SupplyReport",
        String.valueOf(report.getId()),
        "region=" + report.getRegion());
    return toResponse(report, score.value());
  }

  /** Re-scores every report of a farmer (used after FPO validation). */
  @Transactional
  public void rescoreReporterReports(Long reporterId) {
    for (SupplyReport report : reports.findByReporterId(reporterId)) {
      TrustScoreService.Score score =
          scoreFor(report.getReporter(), report, report.getFarm());
      trustScores
          .findBySupplyReportId(report.getId())
          .ifPresent(
              ts -> {
                ts.setScore(score.value());
                ts.setSignals(score.signals());
              });
    }
  }

  private SupplyReport ownedReport(Long reportId, String reporterEmail) {
    User reporter =
        users
            .findByEmail(reporterEmail)
            .orElseThrow(() -> new IllegalArgumentException("Reporter not found"));
    SupplyReport report =
        reports.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));
    if (!report.getReporter().getId().equals(reporter.getId())) {
      throw new AccessDeniedException("Report does not belong to the reporter");
    }
    return report;
  }

  private TrustScoreService.Score scoreFor(User reporter, SupplyReport report, Farm farm) {
    boolean hasEvidence = !evidence.findBySupplyReportId(report.getId()).isEmpty();
    boolean fpoValidated =
        farmerProfiles
            .findByUserId(reporter.getId())
            .map(FarmerProfile::isFpoValidated)
            .orElse(false);
    boolean regionConsistent =
        farmerProfiles
            .findByUserId(reporter.getId())
            .map(
                p ->
                    farm.getRegion() != null
                        && farm.getRegion().equalsIgnoreCase(p.getRegion()))
            .orElse(false);
    return trustScoreService.computeScore(
        reporter.getVerificationStatus(), hasEvidence, fpoValidated, regionConsistent);
  }

  private void ensureActive(User user) {
    if (user.getAccountStatus() != AccountStatus.ACTIVE) {
      throw new AccountLockedException("Account is not active");
    }
  }

  private SupplyReportResponse toResponse(SupplyReport report, double trustScore) {
    return new SupplyReportResponse(
        report.getId(),
        report.getFarm().getId(),
        report.getCrop().getName(),
        report.getQuantityMinTonnes(),
        report.getQuantityMaxTonnes(),
        report.getHarvestStart(),
        report.getHarvestEnd(),
        report.getQuality(),
        report.getRegion(),
        report.getStatus(),
        trustScore);
  }
}
