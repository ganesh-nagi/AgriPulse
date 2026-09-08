package com.agripulse.supply;

import com.agripulse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Optional supporting evidence attached to a supply report (photo ref, receipt note). */
@Entity
@Table(name = "supply_evidence")
public class SupplyEvidence extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "supply_report_id", nullable = false)
  private SupplyReport supplyReport;

  @Column(name = "evidence_type", nullable = false, length = 64)
  private String evidenceType;

  @Column(length = 1024)
  private String reference;

  public SupplyReport getSupplyReport() {
    return supplyReport;
  }

  public void setSupplyReport(SupplyReport supplyReport) {
    this.supplyReport = supplyReport;
  }

  public String getEvidenceType() {
    return evidenceType;
  }

  public void setEvidenceType(String evidenceType) {
    this.evidenceType = evidenceType;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }
}
