package com.agripulse.market;

import com.agripulse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Regional market node (mandi / collection centre) with observed absorption. */
@Entity
@Table(name = "markets")
public class Market extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String region;

  private Double latitude;

  private Double longitude;

  @Column(name = "absorption_min_tonnes")
  private double absorptionMinTonnes;

  @Column(name = "absorption_max_tonnes")
  private double absorptionMaxTonnes;

  @Column(name = "market_type", length = 64)
  private String marketType;

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

  public String getMarketType() {
    return marketType;
  }

  public void setMarketType(String marketType) {
    this.marketType = marketType;
  }
}
