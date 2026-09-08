package com.agripulse.market;

/**
 * Public market node: name, region and observed absorption range only.
 * No trader identities, no exact positions.
 */
public class MarketNodeView {

  private final Long id;
  private final String name;
  private final String region;
  private final double absorptionMinTonnes;
  private final double absorptionMaxTonnes;
  private final String marketType;
  private final Double latitude;
  private final Double longitude;

  public MarketNodeView(
      Long id,
      String name,
      String region,
      double absorptionMinTonnes,
      double absorptionMaxTonnes,
      String marketType,
      Double latitude,
      Double longitude) {
    this.id = id;
    this.name = name;
    this.region = region;
    this.absorptionMinTonnes = absorptionMinTonnes;
    this.absorptionMaxTonnes = absorptionMaxTonnes;
    this.marketType = marketType;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getRegion() {
    return region;
  }

  public double getAbsorptionMinTonnes() {
    return absorptionMinTonnes;
  }

  public double getAbsorptionMaxTonnes() {
    return absorptionMaxTonnes;
  }

  public String getMarketType() {
    return marketType;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }
}
