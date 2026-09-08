package com.agripulse.processing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingFacilityRepository extends JpaRepository<ProcessingFacility, Long> {
  List<ProcessingFacility> findByRegion(String region);
}
