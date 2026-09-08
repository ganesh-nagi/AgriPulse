package com.agripulse.user;

import java.util.Map;

/**
 * Operational analytics for admins. Counts and averages only — no personal
 * data, no coordinates, no individual records.
 */
public class AdminOverviewResponse {

  private final long totalUsers;
  private final Map<String, Long> usersByRole;
  private final Map<String, Long> reportsByStatus;
  private final double averageTrustScore;
  private final long openRequirements;
  private final long storageFacilities;
  private final long transportResources;

  public AdminOverviewResponse(
      long totalUsers,
      Map<String, Long> usersByRole,
      Map<String, Long> reportsByStatus,
      double averageTrustScore,
      long openRequirements,
      long storageFacilities,
      long transportResources) {
    this.totalUsers = totalUsers;
    this.usersByRole = usersByRole;
    this.reportsByStatus = reportsByStatus;
    this.averageTrustScore = averageTrustScore;
    this.openRequirements = openRequirements;
    this.storageFacilities = storageFacilities;
    this.transportResources = transportResources;
  }

  public long getTotalUsers() {
    return totalUsers;
  }

  public Map<String, Long> getUsersByRole() {
    return usersByRole;
  }

  public Map<String, Long> getReportsByStatus() {
    return reportsByStatus;
  }

  public double getAverageTrustScore() {
    return averageTrustScore;
  }

  public long getOpenRequirements() {
    return openRequirements;
  }

  public long getStorageFacilities() {
    return storageFacilities;
  }

  public long getTransportResources() {
    return transportResources;
  }
}
