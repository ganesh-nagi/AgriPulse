package com.agripulse.simulation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
  List<Scenario> findByOwnerId(Long ownerId);
}
