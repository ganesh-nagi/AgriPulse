package com.agripulse.supply;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplyReportRepository extends JpaRepository<SupplyReport, Long> {
  List<SupplyReport> findByReporterId(Long reporterId);

  List<SupplyReport> findByRegionAndStatus(String region, ReportStatus status);

  List<SupplyReport> findByCropIdAndRegionAndStatusIn(
      Long cropId, String region, Collection<ReportStatus> statuses);
}
