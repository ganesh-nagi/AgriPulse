package com.agripulse.processing;

import com.agripulse.common.BaseEntity;
import com.agripulse.crop.Crop;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;

/** Crop-specific processing capacity (cleaning, grading, packing). State only, no allocation. */
@Entity
@Table(name = "processing_facilities")
public class ProcessingFacility extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String region;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "crop_id", nullable = false)
  private Crop crop;

  @Column(name = "capacity_tonnes", nullable = false)
  private double capacityTonnes;

  @Column(name = "occupied_tonnes", nullable = false)
  private double occupiedTonnes;

  @Column(name = "cost_per_day")
  private Double costPerDay;

  @Column(name = "available_from")
  private LocalDate availableFrom;

  @Column(name = "available_to")
  private LocalDate availableTo;

  /** Approximate location, nullable. Only zone-level nodes are exposed. */
  private Double latitude;

  private Double longitude;

  @Transient
  public double getAvailableCapacityTonnes() {
    return Math.max(0.0, capacityTonnes - occupiedTonnes);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public Crop getCrop() {
    return crop;
  }

  public void setCrop(Crop crop) {
    this.crop = crop;
  }

  public double getCapacityTonnes() {
    return capacityTonnes;
  }

  public void setCapacityTonnes(double capacityTonnes) {
    this.capacityTonnes = capacityTonnes;
  }

  public double getOccupiedTonnes() {
    return occupiedTonnes;
  }

  public void setOccupiedTonnes(double occupiedTonnes) {
    this.occupiedTonnes = occupiedTonnes;
  }

  public Double getCostPerDay() {
    return costPerDay;
  }

  public void setCostPerDay(Double costPerDay) {
    this.costPerDay = costPerDay;
  }

  public LocalDate getAvailableFrom() {
    return availableFrom;
  }

  public void setAvailableFrom(LocalDate availableFrom) {
    this.availableFrom = availableFrom;
  }

  public LocalDate getAvailableTo() {
    return availableTo;
  }

  public void setAvailableTo(LocalDate availableTo) {
    this.availableTo = availableTo;
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
}
