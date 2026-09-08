package com.agripulse.supply;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplyReportRepository extends JpaRepository<SupplyReport, Long> {
  List<SupplyReport> findByReporterId(Long reporterId);

  List<SupplyReport> findByRegionAndStatus(String region, ReportStatus status);
}
