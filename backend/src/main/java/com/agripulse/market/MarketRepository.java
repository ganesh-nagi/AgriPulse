package com.agripulse.market;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRepository extends JpaRepository<Market, Long> {
  List<Market> findByRegion(String region);
}
