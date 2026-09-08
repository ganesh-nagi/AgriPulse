package com.agripulse.supply;

import com.agripulse.demand.DataSource;

/**
 * Privacy-safe regional aggregate. No identities, no coordinates — only
 * ranges, counts and average confidence. Every figure is labelled ESTIMATED.
 */
public class RegionalSupplyView {

  private final String region;
  private final String cropName;
  private final long reportCount;
  private final double quantityMinTotal;
  private final double quantityMaxTotal;
  private final double averageConfidence;
  private final DataSource dataSource = DataSource.ESTIMATED;

  public RegionalSupplyView(
      String region,
      String cropName,
      long reportCount,
      double quantityMinTotal,
      double quantityMaxTotal,
      double averageConfidence) {
    this.region = region;
    this.cropName = cropName;
    this.reportCount = reportCount;
    this.quantityMinTotal = quantityMinTotal;
    this.quantityMaxTotal = quantityMaxTotal;
    this.averageConfidence = averageConfidence;
  }

  public String getRegion() {
    return region;
  }

  public String getCropName() {
    return cropName;
  }

  public long getReportCount() {
    return reportCount;
  }

  public double getQuantityMinTotal() {
    return quantityMinTotal;
  }

  public double getQuantityMaxTotal() {
    return quantityMaxTotal;
  }

  public double getAverageConfidence() {
    return averageConfidence;
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}
