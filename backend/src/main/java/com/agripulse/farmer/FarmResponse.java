package com.agripulse.farmer;

/**
 * Owner view of a farm. Coordinates are included because the owner sees their
 * own data; aggregate/public views never use this DTO.
 */
public class FarmResponse {

  private final Long id;
  private final String name;
  private final String region;
  private final Double areaAcres;

  public FarmResponse(Long id, String name, String region, Double areaAcres) {
    this.id = id;
    this.name = name;
    this.region = region;
    this.areaAcres = areaAcres;
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

  public Double getAreaAcres() {
    return areaAcres;
  }
}
