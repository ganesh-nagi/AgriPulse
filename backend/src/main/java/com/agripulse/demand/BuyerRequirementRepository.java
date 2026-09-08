package com.agripulse.demand;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerRequirementRepository extends JpaRepository<BuyerRequirement, Long> {
  List<BuyerRequirement> findByBuyerId(Long buyerProfileId);

  List<BuyerRequirement> findByRegionAndStatus(String region, RequirementStatus status);

  List<BuyerRequirement> findByCropIdAndRegionAndStatusInAndRequiredDateBetween(
      Long cropId,
      String region,
      Collection<RequirementStatus> statuses,
      LocalDate from,
      LocalDate to);
}
