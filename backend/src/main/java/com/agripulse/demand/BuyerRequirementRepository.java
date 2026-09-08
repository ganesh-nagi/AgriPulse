package com.agripulse.demand;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerRequirementRepository extends JpaRepository<BuyerRequirement, Long> {
  List<BuyerRequirement> findByBuyerId(Long buyerProfileId);

  List<BuyerRequirement> findByRegionAndStatus(String region, RequirementStatus status);
}
