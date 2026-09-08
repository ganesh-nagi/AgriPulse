package com.agripulse.transport;

import com.agripulse.common.BaseEntity;
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

/** Truck / pickup operated by a transporter in a region. */
@Entity
@Table(name = "transport_resources")
public class TransportResource extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

  @Column(name = "capacity_tonnes", nullable = false)
  private double capacityTonnes;

  @Column(name = "available_from")
  private LocalDate availableFrom;

  @Column(name = "available_to")
  private LocalDate availableTo;

  @Column(name = "origin_region", nullable = false)
  private String originRegion;

  @Column(name = "dest_region")
  private String destRegion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TransportStatus status = TransportStatus.AVAILABLE;

  @Column(name = "cost_per_km")
  private Double costPerKm;

  /** Optional planner hint; null means unknown. */
  @Column(name = "travel_estimate_days")
  private Double travelEstimateDays;

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }

  public double getCapacityTonnes() {
    return capacityTonnes;
  }

  public void setCapacityTonnes(double capacityTonnes) {
    this.capacityTonnes = capacityTonnes;
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

  public String getOriginRegion() {
    return originRegion;
  }

  public void setOriginRegion(String originRegion) {
    this.originRegion = originRegion;
  }

  public String getDestRegion() {
    return destRegion;
  }

  public void setDestRegion(String destRegion) {
    this.destRegion = destRegion;
  }

  public TransportStatus getStatus() {
    return status;
  }

  public void setStatus(TransportStatus status) {
    this.status = status;
  }

  public Double getCostPerKm() {
    return costPerKm;
  }

  public void setCostPerKm(Double costPerKm) {
    this.costPerKm = costPerKm;
  }

  public Double getTravelEstimateDays() {
    return travelEstimateDays;
  }

  public void setTravelEstimateDays(Double travelEstimateDays) {
    this.travelEstimateDays = travelEstimateDays;
  }
}
