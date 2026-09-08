package com.agripulse.trust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agripulse.user.VerificationStatus;
import org.junit.jupiter.api.Test;

class TrustScoreServiceTest {

  private final TrustScoreService service = new TrustScoreService();

  @Test
  void unverifiedReportWithoutEvidenceGetsBaseScore() {
    TrustScoreService.Score score =
        service.computeInitialScore(VerificationStatus.UNVERIFIED, false, false);
    assertEquals(20.0, score.value());
  }

  @Test
  void fullyVerifiedReportWithEvidenceIsCappedBelowHundred() {
    TrustScoreService.Score score =
        service.computeInitialScore(VerificationStatus.FPO_VALIDATED, true, true);
    assertTrue(score.value() <= 95.0);
    assertEquals(90.0, score.value());
  }

  @Test
  void identityVerificationAddsTwenty() {
    TrustScoreService.Score score =
        service.computeInitialScore(VerificationStatus.IDENTITY_VERIFIED, false, false);
    assertEquals(50.0, score.value());
  }
}
