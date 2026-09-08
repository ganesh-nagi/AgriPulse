package com.agripulse.transport;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportResourceRepository extends JpaRepository<TransportResource, Long> {
  List<TransportResource> findByOriginRegionAndStatus(String originRegion, TransportStatus status);

  List<TransportResource> findByOwnerId(Long ownerId);
}
