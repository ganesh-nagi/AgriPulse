package com.agripulse.supply;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplyEvidenceRepository extends JpaRepository<SupplyEvidence, Long> {
  List<SupplyEvidence> findBySupplyReportId(Long supplyReportId);
}
