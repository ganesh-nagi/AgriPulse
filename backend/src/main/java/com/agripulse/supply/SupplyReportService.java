package com.agripulse.supply;

import com.agripulse.audit.AuditService;
import com.agripulse.auth.AccountLockedException;
import com.agripulse.crop.Crop;
import com.agripulse.crop.CropRepository;
import com.agripulse.farmer.Farm;
import com.agripulse.farmer.FarmRepository;
import com.agripulse.farmer.FarmerProfile;
import com.agripulse.farmer.FarmerProfileRepository;
import com.agripulse.trust.TrustLevel;
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
    validateRange(
        request.getQuantityMinTonnes(),
        request.getQuantityMaxTonnes(),
        request.getHarvestStart(),
        request.getHarvestEnd());
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
    // Suspicious reports stay SUBMITTED (never auto-rejected); the trust
    // assessment routes them to review via the stored review flag.
    report.setStatus(ReportStatus.SUBMITTED);
    reports.save(report);

    TrustScore trustScore = persistAssessment(report);

    auditService.record(
        reporter.getId(),
        "SUPPLY_REPORT_CREATED",
        "SupplyReport",
        String.valueOf(report.getId()),
        "region=" + report.getRegion());
    return toResponse(report, trustScore);
  }

  @Transactional(readOnly = true)
  public List<SupplyReportResponse> listMine(String reporterEmail) {
    User reporter =
        users
            .findByEmail(reporterEmail)
            .orElseThrow(() -> new IllegalArgumentException("Reporter not found"));
    return reports.findByReporterId(reporter.getId()).stream()
        .map(r -> toResponse(r, trustScores.findBySupplyReportId(r.getId()).orElse(null)))
        .toList();
  }

  /** Owner-scoped single read. Other farmers can never fetch this. */
  @Transactional(readOnly = true)
  public SupplyReportResponse getOne(Long reportId, String reporterEmail) {
    SupplyReport report = ownedReport(reportId, reporterEmail);
    return toResponse(report, trustScores.findBySupplyReportId(report.getId()).orElse(null));
  }

  /** Owner update of their own DRAFT/SUBMITTED report; re-scored afterwards. */
  @Transactional
  public SupplyReportResponse updateOwn(
      Long reportId, UpdateSupplyReportRequest request, String reporterEmail) {
    SupplyReport report = ownedReport(reportId, reporterEmail);
    ensureActive(report.getReporter());
    if (report.getStatus() != ReportStatus.DRAFT
        && report.getStatus() != ReportStatus.SUBMITTED) {
      throw new IllegalArgumentException("Only draft or submitted reports can be edited");
    }
    validateRange(
        request.getQuantityMinTonnes(),
        request.getQuantityMaxTonnes(),
        request.getHarvestStart(),
        request.getHarvestEnd());
    report.setQuantityMinTonnes(request.getQuantityMinTonnes());
    report.setQuantityMaxTonnes(request.getQuantityMaxTonnes());
    report.setHarvestStart(request.getHarvestStart());
    report.setHarvestEnd(request.getHarvestEnd());
    report.setQuality(request.getQuality());
    TrustScore trustScore = persistAssessment(report);
    auditService.record(
        report.getReporter().getId(),
        "SUPPLY_REPORT_UPDATED",
        "SupplyReport",
        String.valueOf(report.getId()),
        "region=" + report.getRegion());
    return toResponse(report, trustScore);
  }

  /** Owner cancel of their own DRAFT/SUBMITTED report. Cancelled reports leave aggregates. */
  @Transactional
  public SupplyReportResponse cancelOwn(Long reportId, String reporterEmail) {
    SupplyReport report = ownedReport(reportId, reporterEmail);
    ensureActive(report.getReporter());
    if (report.getStatus() != ReportStatus.DRAFT
        && report.getStatus() != ReportStatus.SUBMITTED) {
      throw new IllegalArgumentException("Only draft or submitted reports can be cancelled");
    }
    report.setStatus(ReportStatus.CANCELLED);
    auditService.record(
        report.getReporter().getId(),
        "SUPPLY_REPORT_CANCELLED",
        "SupplyReport",
        String.valueOf(report.getId()),
        "region=" + report.getRegion());
    TrustScore trustScore = trustScores.findBySupplyReportId(report.getId()).orElse(null);
    return toResponse(report, trustScore);
  }

  /** Re-scores every report of a farmer (used after FPO validation). */
  @Transactional
  public void rescoreReporterReports(Long reporterId) {
    for (SupplyReport report : reports.findByReporterId(reporterId)) {
      persistAssessment(report);
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

  /**
   * Runs the trust engine for a report and stores the assessment. The stored
   * confidence is the report's effective influence weight for future regional
   * aggregation; the review flag routes suspicious reports to human review.
   */
  private TrustScore persistAssessment(SupplyReport report) {
    User reporter = report.getReporter();
    Farm farm = report.getFarm();
    double[] history = pastAverageConfidence(reporter.getId(), report.getId());
    TrustScoreService.TrustAssessment assessment =
        trustScoreService.assess(
            new TrustScoreService.TrustSignals(
                reporter.getVerificationStatus(),
                !evidence.findBySupplyReportId(report.getId()).isEmpty(),
                isFpoValidated(reporter),
                isRegionConsistent(reporter, farm),
                (int) history[0],
                history[1],
                quantityDeviationRatio(report),
                isTemporalConsistent(report),
                isCrossSourceConsistent(report)));
    TrustScore trustScore =
        trustScores.findBySupplyReportId(report.getId()).orElseGet(TrustScore::new);
    trustScore.setSupplyReport(report);
    trustScore.setScore(assessment.confidence());
    trustScore.setSignals(assessment.signals());
    trustScore.setLevel(assessment.level());
    trustScore.setRequiresReview(assessment.requiresReview());
    return trustScores.save(trustScore);
  }

  private boolean isFpoValidated(User reporter) {
    return farmerProfiles
        .findByUserId(reporter.getId())
        .map(FarmerProfile::isFpoValidated)
        .orElse(false);
  }

  private boolean isRegionConsistent(User reporter, Farm farm) {
    return farmerProfiles
        .findByUserId(reporter.getId())
        .map(p -> farm.getRegion() != null && farm.getRegion().equalsIgnoreCase(p.getRegion()))
        .orElse(false);
  }

  /** Returns [pastReportCount, pastAverageConfidence], excluding the current report. */
  private double[] pastAverageConfidence(Long reporterId, Long currentReportId) {
    List<SupplyReport> past =
        reports.findByReporterId(reporterId).stream()
            .filter(r -> !r.getId().equals(currentReportId))
            .toList();
    double sum = 0.0;
    int scored = 0;
    for (SupplyReport r : past) {
      var ts = trustScores.findBySupplyReportId(r.getId());
      if (ts.isPresent()) {
        sum += ts.get().getScore();
        scored++;
      }
    }
    return new double[] {past.size(), scored == 0 ? 0.0 : sum / scored};
  }

  /**
   * Reported midpoint divided by the peer median for the same crop and
   * region (SUBMITTED/VALIDATED, excluding this report). Non-positive means
   * "unknown" and stays neutral in the engine.
   */
  private double quantityDeviationRatio(SupplyReport report) {
    List<Double> midpoints =
        reports
            .findByCropIdAndRegionAndStatusIn(
                report.getCrop().getId(),
                report.getRegion(),
                List.of(ReportStatus.SUBMITTED, ReportStatus.VALIDATED))
            .stream()
            .filter(r -> !r.getId().equals(report.getId()))
            .map(r -> (r.getQuantityMinTonnes() + r.getQuantityMaxTonnes()) / 2.0)
            .sorted()
            .toList();
    if (midpoints.isEmpty()) {
      return -1.0;
    }
    double median =
        midpoints.size() % 2 == 1
            ? midpoints.get(midpoints.size() / 2)
            : (midpoints.get(midpoints.size() / 2 - 1) + midpoints.get(midpoints.size() / 2)) / 2.0;
    if (median <= 0) {
      return -1.0;
    }
    double midpoint =
        (report.getQuantityMinTonnes() + report.getQuantityMaxTonnes()) / 2.0;
    return midpoint / median;
  }

  /** Sane harvest window: ordered, 1-180 days, start within [-60, +365] days of today. */
  private boolean isTemporalConsistent(SupplyReport report) {
    if (report.getHarvestStart() == null
        || report.getHarvestEnd() == null
        || report.getHarvestStart().isAfter(report.getHarvestEnd())) {
      return false;
    }
    long windowDays =
        java.time.temporal.ChronoUnit.DAYS.between(report.getHarvestStart(), report.getHarvestEnd());
    if (windowDays < 1 || windowDays > 180) {
      return false;
    }
    java.time.LocalDate today = java.time.LocalDate.now();
    return !report.getHarvestStart().isBefore(today.minusDays(60))
        && !report.getHarvestStart().isAfter(today.plusDays(365));
  }

  /** Aligned with peers on quantity and quality, when peers exist. */
  private boolean isCrossSourceConsistent(SupplyReport report) {
    double ratio = quantityDeviationRatio(report);
    if (ratio <= 0 || ratio < 0.5 || ratio > 2.0) {
      return false;
    }
    List<SupplyReport> peers =
        reports
            .findByCropIdAndRegionAndStatusIn(
                report.getCrop().getId(),
                report.getRegion(),
                List.of(ReportStatus.SUBMITTED, ReportStatus.VALIDATED))
            .stream()
            .filter(r -> !r.getId().equals(report.getId()))
            .toList();
    if (peers.isEmpty()) {
      return false;
    }
    if (report.getQuality() == null) {
      return true;
    }
    return peers.stream()
        .anyMatch(
            p -> p.getQuality() != null && p.getQuality().equalsIgnoreCase(report.getQuality()));
  }

  private void ensureActive(User user) {
    if (user.getAccountStatus() != AccountStatus.ACTIVE) {
      throw new AccountLockedException("Account is not active");
    }
  }

  private void validateRange(
      double quantityMin,
      double quantityMax,
      java.time.LocalDate harvestStart,
      java.time.LocalDate harvestEnd) {
    if (quantityMin < 0) {
      throw new IllegalArgumentException("quantityMin must be >= 0");
    }
    if (quantityMax <= 0) {
      throw new IllegalArgumentException("quantityMax must be greater than 0");
    }
    if (quantityMin > quantityMax) {
      throw new IllegalArgumentException("quantityMin must be <= quantityMax");
    }
    if (harvestStart == null || harvestEnd == null) {
      throw new IllegalArgumentException("harvestStart and harvestEnd are required");
    }
    if (harvestStart.isAfter(harvestEnd)) {
      throw new IllegalArgumentException("harvestStart must be on or before harvestEnd");
    }
  }

  private SupplyReportResponse toResponse(SupplyReport report, TrustScore trustScore) {
    // Every report is assessed atomically at create/update time, so a stored
    // score always exists; the fallback below is unreachable in practice.
    double confidence = trustScore != null ? trustScore.getScore() : 0.0;
    String level =
        trustScore != null && trustScore.getLevel() != null
            ? trustScore.getLevel().name()
            : TrustLevel.MEDIUM_CONFIDENCE.name();
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
        confidence,
        level);
  }
}
