package com.agripulse.supply;

import com.agripulse.common.BaseEntity;
import com.agripulse.crop.Crop;
import com.agripulse.farmer.Farm;
import com.agripulse.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A farmer's expected supply signal. Quantities are ranges, never exact truth.
 * Exact coordinates stay server-side; aggregation APIs expose region only.
 */
@Entity
@Table(name = "supply_reports")
public class SupplyReport extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "farm_id", nullable = false)
  private Farm farm;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "crop_id", nullable = false)
  private Crop crop;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reporter_id", nullable = false)
  private User reporter;

  @Column(name = "quantity_min_tonnes", nullable = false)
  private double quantityMinTonnes;

  @Column(name = "quantity_max_tonnes", nullable = false)
  private double quantityMaxTonnes;

  @Column(name = "harvest_start", nullable = false)
  private LocalDate harvestStart;

  @Column(name = "harvest_end", nullable = false)
  private LocalDate harvestEnd;

  @Column(length = 32)
  private String quality;

  @Column(nullable = false)
  private String region;

  private Double latitude;

  private Double longitude;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ReportStatus status = ReportStatus.DRAFT;

  public Farm getFarm() {
    return farm;
  }

  public void setFarm(Farm farm) {
    this.farm = farm;
  }

  public Crop getCrop() {
    return crop;
  }

  public void setCrop(Crop crop) {
    this.crop = crop;
  }

  public User getReporter() {
    return reporter;
  }

  public void setReporter(User reporter) {
    this.reporter = reporter;
  }

  public double getQuantityMinTonnes() {
    return quantityMinTonnes;
  }

  public void setQuantityMinTonnes(double quantityMinTonnes) {
    this.quantityMinTonnes = quantityMinTonnes;
  }

  public double getQuantityMaxTonnes() {
    return quantityMaxTonnes;
  }

  public void setQuantityMaxTonnes(double quantityMaxTonnes) {
    this.quantityMaxTonnes = quantityMaxTonnes;
  }

  public LocalDate getHarvestStart() {
    return harvestStart;
  }

  public void setHarvestStart(LocalDate harvestStart) {
    this.harvestStart = harvestStart;
  }

  public LocalDate getHarvestEnd() {
    return harvestEnd;
  }

  public void setHarvestEnd(LocalDate harvestEnd) {
    this.harvestEnd = harvestEnd;
  }

  public String getQuality() {
    return quality;
  }

  public void setQuality(String quality) {
    this.quality = quality;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public ReportStatus getStatus() {
    return status;
  }

  public void setStatus(ReportStatus status) {
    this.status = status;
  }
}
