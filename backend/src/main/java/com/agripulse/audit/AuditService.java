package com.agripulse.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records security/business actions. Callers must never pass secrets or personal data. */
@Service
public class AuditService {

  private final AuditLogRepository auditLogRepository;

  public AuditService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional
  public void record(
      Long actorId, String action, String entityType, String entityId, String details) {
    AuditLog log = new AuditLog();
    log.setActorId(actorId);
    log.setAction(action);
    log.setEntityType(entityType);
    log.setEntityId(entityId);
    log.setDetails(details);
    auditLogRepository.save(log);
  }
}
