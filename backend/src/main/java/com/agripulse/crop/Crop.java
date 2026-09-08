package com.agripulse.crop;

import com.agripulse.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Reference crop. MVP scope: Tomato. */
@Entity
@Table(name = "crops")
public class Crop extends BaseEntity {

  @Column(nullable = false, unique = true, length = 64)
  private String name;

  @Column(nullable = false, length = 16)
  private String unit = "tonne";

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }
}
