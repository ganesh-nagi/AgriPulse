package com.agripulse.trust;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustScoreRepository extends JpaRepository<TrustScore, Long> {
  Optional<TrustScore> findBySupplyReportId(Long supplyReportId);
}
