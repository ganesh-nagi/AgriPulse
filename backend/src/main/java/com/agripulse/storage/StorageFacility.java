package com.agripulse.storage;

import com.agripulse.common.BaseEntity;
import com.agripulse.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;

/** Optional storage resource. Availability is never assumed. */
@Entity
@Table(name = "storage_facilities")
public class StorageFacility extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String region;

  private Double latitude;

  private Double longitude;

  @Column(name = "capacity_tonnes", nullable = false)
  private double capacityTonnes;

  @Column(name = "occupied_tonnes", nullable = false)
  private double occupiedTonnes;

  @Column(name = "crop_compatibility", length = 256)
  private String cropCompatibility;

  @Column(name = "cost_per_day")
  private Double costPerDay;

  @Column(name = "available_from")
  private LocalDate availableFrom;

  @Column(name = "available_to")
  private LocalDate availableTo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "operator_id")
  private User operator;

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

  public String getCropCompatibility() {
    return cropCompatibility;
  }

  public void setCropCompatibility(String cropCompatibility) {
    this.cropCompatibility = cropCompatibility;
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

  public User getOperator() {
    return operator;
  }

  public void setOperator(User operator) {
    this.operator = operator;
  }
}
