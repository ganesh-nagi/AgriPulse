package com.agripulse.trust;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRecordRepository extends JpaRepository<VerificationRecord, Long> {
  List<VerificationRecord> findBySubjectId(Long subjectUserId);
}
