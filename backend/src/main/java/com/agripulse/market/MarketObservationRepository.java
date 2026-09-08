package com.agripulse.market;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketObservationRepository extends JpaRepository<MarketObservation, Long> {
  List<MarketObservation> findByCropId(Long cropId);
}
