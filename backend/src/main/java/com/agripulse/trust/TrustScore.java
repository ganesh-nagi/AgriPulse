package com.agripulse.trust;

import com.agripulse.common.BaseEntity;
import com.agripulse.supply.SupplyReport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Confidence attached to one supply report (0-100).
 * Low confidence reduces the report's influence on regional estimates.
 */
@Entity
@Table(name = "trust_scores")
public class TrustScore extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "supply_report_id", nullable = false, unique = true)
  private SupplyReport supplyReport;

  @Column(nullable = false)
  private double score;

  @Column(length = 2000)
  private String signals;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private TrustLevel level;

  @Column(name = "requires_review", nullable = false)
  private boolean requiresReview = false;

  @Column(name = "computed_at", nullable = false)
  private LocalDateTime computedAt = LocalDateTime.now();

  public SupplyReport getSupplyReport() {
    return supplyReport;
  }

  public void setSupplyReport(SupplyReport supplyReport) {
    this.supplyReport = supplyReport;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  public String getSignals() {
    return signals;
  }

  public void setSignals(String signals) {
    this.signals = signals;
  }

  public LocalDateTime getComputedAt() {
    return computedAt;
  }

  public void setComputedAt(LocalDateTime computedAt) {
    this.computedAt = computedAt;
  }

  public TrustLevel getLevel() {
    return level;
  }

  public void setLevel(TrustLevel level) {
    this.level = level;
  }

  public boolean isRequiresReview() {
    return requiresReview;
  }

  public void setRequiresReview(boolean requiresReview) {
    this.requiresReview = requiresReview;
  }
}
