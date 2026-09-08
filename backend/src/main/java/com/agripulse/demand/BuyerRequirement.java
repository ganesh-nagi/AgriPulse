package com.agripulse.demand;

import com.agripulse.buyer.BuyerProfile;
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
import java.time.LocalDate;

/** Confirmed buyer demand signal. One row = one requirement. */
@Entity
@Table(name = "buyer_requirements")
public class BuyerRequirement extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "buyer_profile_id", nullable = false)
  private BuyerProfile buyer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "crop_id", nullable = false)
  private Crop crop;

  @Column(name = "quantity_tonnes", nullable = false)
  private double quantityTonnes;

  @Column(length = 32)
  private String quality;

  @Column(name = "required_date", nullable = false)
  private LocalDate requiredDate;

  @Column(nullable = false)
  private String region;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private RequirementStatus status = RequirementStatus.OPEN;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_source", nullable = false, length = 16)
  private DataSource dataSource = DataSource.REAL;

  public BuyerProfile getBuyer() {
    return buyer;
  }

  public void setBuyer(BuyerProfile buyer) {
    this.buyer = buyer;
  }

  public Crop getCrop() {
    return crop;
  }

  public void setCrop(Crop crop) {
    this.crop = crop;
  }

  public double getQuantityTonnes() {
    return quantityTonnes;
  }

  public void setQuantityTonnes(double quantityTonnes) {
    this.quantityTonnes = quantityTonnes;
  }

  public String getQuality() {
    return quality;
  }

  public void setQuality(String quality) {
    this.quality = quality;
  }

  public LocalDate getRequiredDate() {
    return requiredDate;
  }

  public void setRequiredDate(LocalDate requiredDate) {
    this.requiredDate = requiredDate;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public RequirementStatus getStatus() {
    return status;
  }

  public void setStatus(RequirementStatus status) {
    this.status = status;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }
}
