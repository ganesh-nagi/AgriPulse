package com.agripulse.fpo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FpoProfileRepository extends JpaRepository<FpoProfile, Long> {
  Optional<FpoProfile> findByUserId(Long userId);
}
