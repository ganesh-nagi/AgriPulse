package com.agripulse.buyer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerProfileRepository extends JpaRepository<BuyerProfile, Long> {
  Optional<BuyerProfile> findByUserId(Long userId);
}
