package com.agripulse.crop;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropRepository extends JpaRepository<Crop, Long> {
  Optional<Crop> findByName(String name);
}
