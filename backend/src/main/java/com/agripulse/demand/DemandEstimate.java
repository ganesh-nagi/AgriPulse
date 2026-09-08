package com.agripulse.demand;

import com.agripulse.common.BaseEntity;
import com.agripulse.crop.Crop;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Regional demand picture: confirmed buyer requirements plus observed
 * historical market absorption, combined into an estimated range with
 * an explicit confidence level. Never exact future demand.
 */
@Entity
@Table(name = "demand_estimates")
public class DemandEstimate extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "crop_id", nullable = false)
  private Crop crop;

  @Column(nullable = false)
  private String region;

  @Column(name = "confirmed_demand_tonnes", nullable = false)
  private double confirmedDemandTonnes;

  @Column(name = "absorption_min_tonnes", nullable = false)
  private double absorptionMinTonnes;

  @Column(name = "absorption_max_tonnes", nullable = false)
  private double absorptionMaxTonnes;

  @Column(name = "estimated_min_tonnes", nullable = false)
  private double estimatedMinTonnes;

  @Column(name = "estimated_max_tonnes", nullable = false)
  private double estimatedMaxTonnes;

  /** 0-100. Never 100: estimates carry uncertainty by definition. */
  @Column(nullable = false)
  private double confidence;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_source", nullable = false, length = 16)
  private DataSource dataSource = DataSource.ESTIMATED;

  @Column(name = "computed_at", nullable = false)
  private LocalDateTime computedAt = LocalDateTime.now();

  public Crop getCrop() {
    return crop;
  }

  public void setCrop(Crop crop) {
    this.crop = crop;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public double getConfirmedDemandTonnes() {
    return confirmedDemandTonnes;
  }

  public void setConfirmedDemandTonnes(double confirmedDemandTonnes) {
    this.confirmedDemandTonnes = confirmedDemandTonnes;
  }

  public double getAbsorptionMinTonnes() {
    return absorptionMinTonnes;
  }

  public void setAbsorptionMinTonnes(double absorptionMinTonnes) {
    this.absorptionMinTonnes = absorptionMinTonnes;
  }

  public double getAbsorptionMaxTonnes() {
    return absorptionMaxTonnes;
  }

  public void setAbsorptionMaxTonnes(double absorptionMaxTonnes) {
    this.absorptionMaxTonnes = absorptionMaxTonnes;
  }

  public double getEstimatedMinTonnes() {
    return estimatedMinTonnes;
  }

  public void setEstimatedMinTonnes(double estimatedMinTonnes) {
    this.estimatedMinTonnes = estimatedMinTonnes;
  }

  public double getEstimatedMaxTonnes() {
    return estimatedMaxTonnes;
  }

  public void setEstimatedMaxTonnes(double estimatedMaxTonnes) {
    this.estimatedMaxTonnes = estimatedMaxTonnes;
  }

  public double getConfidence() {
    return confidence;
  }

  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public LocalDateTime getComputedAt() {
    return computedAt;
  }

  public void setComputedAt(LocalDateTime computedAt) {
    this.computedAt = computedAt;
  }
}
