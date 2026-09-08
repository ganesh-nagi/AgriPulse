package com.agripulse.pressure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PressureServiceTest {

  private final PressureService service = new PressureService();

  @Test
  void lowSupplyGivesLowBand() {
    assertEquals(PressureBand.LOW, service.compute(40.0, 70.0).band());
  }

  @Test
  void supplyAboveAbsorptionGivesCriticalBand() {
    assertEquals(PressureBand.CRITICAL, service.compute(110.0, 70.0).band());
  }

  @Test
  void zeroAbsorptionDoesNotDivideByZero() {
    assertEquals(PressureBand.CRITICAL, service.compute(10.0, 0.0).band());
  }
}
