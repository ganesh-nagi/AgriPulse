package com.agripulse.supply;

import com.agripulse.trust.TrustScore;
import com.agripulse.trust.TrustScoreRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates SUBMITTED/VALIDATED reports into regional views.
 * Individual reports, farmers and coordinates never leave this service.
 */
@Service
public class RegionalSupplyService {

  private final SupplyReportRepository reports;
  private final TrustScoreRepository trustScores;

  public RegionalSupplyService(
      SupplyReportRepository reports, TrustScoreRepository trustScores) {
    this.reports = reports;
    this.trustScores = trustScores;
  }

  @Transactional(readOnly = true)
  public List<RegionalSupplyView> aggregateByRegion(String region) {
    List<SupplyReport> visible = new ArrayList<>();
    visible.addAll(reports.findByRegionAndStatus(region, ReportStatus.SUBMITTED));
    visible.addAll(reports.findByRegionAndStatus(region, ReportStatus.VALIDATED));
    Map<String, List<SupplyReport>> byCrop = new LinkedHashMap<>();
    for (SupplyReport report : visible) {
      byCrop.computeIfAbsent(report.getCrop().getName(), k -> new ArrayList<>()).add(report);
    }
    List<RegionalSupplyView> views = new ArrayList<>();
    for (Map.Entry<String, List<SupplyReport>> entry : byCrop.entrySet()) {
      double min = 0.0;
      double max = 0.0;
      double confidenceSum = 0.0;
      for (SupplyReport report : entry.getValue()) {
        min += report.getQuantityMinTonnes();
        max += report.getQuantityMaxTonnes();
        confidenceSum +=
            trustScores
                .findBySupplyReportId(report.getId())
                .map(TrustScore::getScore)
                .orElse(0.0);
      }
      double avg =
          entry.getValue().isEmpty() ? 0.0 : confidenceSum / entry.getValue().size();
      views.add(
          new RegionalSupplyView(
              region, entry.getKey(), entry.getValue().size(), min, max, avg));
    }
    return views;
  }
}
