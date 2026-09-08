package com.agripulse.storage;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageFacilityRepository extends JpaRepository<StorageFacility, Long> {
  List<StorageFacility> findByRegion(String region);

  List<StorageFacility> findByOperatorId(Long operatorId);
}
