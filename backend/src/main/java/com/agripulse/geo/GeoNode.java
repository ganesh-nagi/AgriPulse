package com.agripulse.geo;

/**
 * One mappable public node. Zone/market-level locations only: no farmer
 * identities, no farm coordinates, no private quantities.
 */
public class GeoNode {

  private final GeoKind kind;
  private final Long id;
  private final String name;
  private final String region;
  private final double latitude;
  private final double longitude;
  private final double distanceKm;
  private final String summary;
  private final Double availableTonnes;

  public GeoNode(
      GeoKind kind,
      Long id,
      String name,
      String region,
      double latitude,
      double longitude,
      double distanceKm,
      String summary,
      Double availableTonnes) {
    this.kind = kind;
    this.id = id;
    this.name = name;
    this.region = region;
    this.latitude = latitude;
    this.longitude = longitude;
    this.distanceKm = distanceKm;
    this.summary = summary;
    this.availableTonnes = availableTonnes;
  }

  public GeoKind getKind() {
    return kind;
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

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public double getDistanceKm() {
    return distanceKm;
  }

  public String getSummary() {
    return summary;
  }

  public Double getAvailableTonnes() {
    return availableTonnes;
  }
}
