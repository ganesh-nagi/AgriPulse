package com.agripulse.audit;

import com.agripulse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Append-only security/business audit trail.
 * Never stores passwords, tokens or personal sensitive data.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

  @Column(name = "actor_id")
  private Long actorId;

  @Column(nullable = false, length = 64)
  private String action;

  @Column(name = "entity_type", length = 64)
  private String entityType;

  @Column(name = "entity_id", length = 64)
  private String entityId;

  @Column(length = 2000)
  private String details;

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long actorId) {
    this.actorId = actorId;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }
}
