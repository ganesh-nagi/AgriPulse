package com.agripulse.trust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agripulse.trust.TrustScoreService.TrustAssessment;
import com.agripulse.trust.TrustScoreService.TrustSignals;
import com.agripulse.user.VerificationStatus;
import org.junit.jupiter.api.Test;

class TrustEngineTest {

  private final TrustScoreService service = new TrustScoreService();

  @Test
  void highTrustReport() {
    TrustAssessment out = service.assess(strongSignals());
    assertEquals(TrustLevel.HIGH_CONFIDENCE, out.level());
    assertFalse(out.requiresReview());
    assertTrue(out.confidence() <= 95.0);
  }

  @Test
  void lowTrustReportNeedsReviewNotFraud() {
    TrustSignals in =
        new TrustSignals(
            VerificationStatus.UNVERIFIED,
            false,
            false,
            false,
            0,
            0.0,
            4.0,
            false,
            false);
    TrustAssessment out = service.assess(in);
    assertTrue(
        out.level() == TrustLevel.LOW_CONFIDENCE || out.level() == TrustLevel.REQUIRES_REVIEW);
    assertTrue(out.requiresReview());
    assertTrue(out.confidence() >= 0.0);
  }

  @Test
  void missingEvidenceAloneDoesNotReject() {
    TrustSignals in =
        new TrustSignals(
            VerificationStatus.IDENTITY_VERIFIED,
            false,
            true,
            true,
            4,
            75.0,
            1.0,
            true,
            true);
    TrustAssessment out = service.assess(in);
    assertTrue(
        out.level() == TrustLevel.HIGH_CONFIDENCE || out.level() == TrustLevel.MEDIUM_CONFIDENCE);
    assertFalse(out.requiresReview());
  }

  @Test
  void verifiedFpoScoresAboveUnverified() {
    TrustAssessment fpo = service.assess(strongSignals());
    TrustSignals weaker =
        new TrustSignals(
            VerificationStatus.UNVERIFIED,
            true,
            false,
            true,
            4,
            75.0,
            1.0,
            true,
            true);
    TrustAssessment plain = service.assess(weaker);
    assertTrue(fpo.confidence() > plain.confidence());
  }

  @Test
  void largeDeviationReducesConfidence() {
    TrustAssessment aligned = service.assess(moderateSignals());
    TrustSignals deviant =
        new TrustSignals(
            VerificationStatus.PHONE_VERIFIED,
            true,
            false,
            true,
            2,
            60.0,
            5.0,
            true,
            true);
    TrustAssessment out = service.assess(deviant);
    assertTrue(out.confidence() < aligned.confidence());
    assertTrue(out.requiresReview());
  }

  @Test
  void newFarmerScoresBelowExperiencedFarmer() {
    TrustAssessment experienced = service.assess(moderateSignals());
    TrustSignals firstReport =
        new TrustSignals(
            VerificationStatus.PHONE_VERIFIED,
            true,
            false,
            true,
            0,
            0.0,
            1.0,
            true,
            true);
    TrustAssessment newcomer = service.assess(firstReport);
    assertTrue(newcomer.confidence() < experienced.confidence());
  }

  @Test
  void regionInconsistencyReducesConfidence() {
    TrustAssessment consistent = service.assess(moderateSignals());
    TrustSignals inconsistent =
        new TrustSignals(
            VerificationStatus.PHONE_VERIFIED,
            true,
            false,
            false,
            2,
            60.0,
            1.0,
            true,
            true);
    TrustAssessment out = service.assess(inconsistent);
    assertTrue(out.confidence() < consistent.confidence());
  }

  @Test
  void repeatedCalculationIsDeterministic() {
    TrustSignals in = strongSignals();
    TrustAssessment first = service.assess(in);
    TrustAssessment second = service.assess(in);
    assertEquals(first.confidence(), second.confidence());
    assertEquals(first.level(), second.level());
    assertEquals(first.requiresReview(), second.requiresReview());
    assertEquals(first.signals(), second.signals());
  }

  private TrustSignals strongSignals() {
    return new TrustSignals(
        VerificationStatus.IDENTITY_VERIFIED,
        true,
        true,
        true,
        4,
        75.0,
        1.0,
        true,
        true);
  }

  private TrustSignals moderateSignals() {
    return new TrustSignals(
        VerificationStatus.PHONE_VERIFIED,
        true,
        false,
        true,
        2,
        60.0,
        1.0,
        true,
        true);
  }
}
