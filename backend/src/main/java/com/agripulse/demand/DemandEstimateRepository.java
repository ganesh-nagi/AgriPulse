package com.agripulse.demand;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandEstimateRepository extends JpaRepository<DemandEstimate, Long> {
  List<DemandEstimate> findByRegion(String region);
}
