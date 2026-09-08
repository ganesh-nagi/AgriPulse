package com.agripulse.market;

import com.agripulse.common.BaseEntity;
import com.agripulse.crop.Crop;
import com.agripulse.demand.DataSource;
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
 * Historical market activity: observed arrival/trade/absorption for one crop
 * at one market over one period. MVP rows are imported/seeded (see
 * db/migration/V4__market_observations.sql); the service never hard-codes
 * figures. Every row carries its provenance (REAL import or SIMULATED seed).
 */
@Entity
@Table(name = "market_observations")
public class MarketObservation extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "market_id", nullable = false)
  private Market market;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "crop_id", nullable = false)
  private Crop crop;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "absorption_min_tonnes", nullable = false)
  private double absorptionMinTonnes;

  @Column(name = "absorption_max_tonnes", nullable = false)
  private double absorptionMaxTonnes;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_source", nullable = false, length = 16)
  private DataSource dataSource = DataSource.REAL;

  public Market getMarket() {
    return market;
  }

  public void setMarket(Market market) {
    this.market = market;
  }

  public Crop getCrop() {
    return crop;
  }

  public void setCrop(Crop crop) {
    this.crop = crop;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public void setPeriodStart(LocalDate periodStart) {
    this.periodStart = periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public void setPeriodEnd(LocalDate periodEnd) {
    this.periodEnd = periodEnd;
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

  public DataSource getDataSource() {
    return dataSource;
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }
}
