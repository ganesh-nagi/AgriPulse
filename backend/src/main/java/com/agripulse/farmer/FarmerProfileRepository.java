package com.agripulse.farmer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmerProfileRepository extends JpaRepository<FarmerProfile, Long> {
  Optional<FarmerProfile> findByUserId(Long userId);
}
