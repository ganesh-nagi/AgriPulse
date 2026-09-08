package com.agripulse.trust;

import com.agripulse.user.User;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Demo verification provider. Generates clearly labelled MOCK results and a
 * demo OTP that is returned in the API response (a real SMS provider would
 * send it instead). Deterministic demo rule: identity names starting with
 * "Review " are routed to manual review so both paths are testable.
 *
 * <p>NEVER present MOCK results as government KYC.
 */
@Component
public class MockVerificationProvider implements VerificationProvider {

  private final AtomicLong sequence = new AtomicLong(1);
  private final SecureRandom random = new SecureRandom();

  @Override
  public String providerName() {
    return "MOCK";
  }

  @Override
  public VerificationResult verifyPhone(User user, String phone) {
    if (phone == null || phone.isBlank()) {
      return new VerificationResult(
          VerificationDecision.REJECTED, ref("SMS"), null, "MOCK: phone number missing");
    }
    String code = String.format("%06d", random.nextInt(1_000_000));
    return new VerificationResult(
        VerificationDecision.PENDING, ref("SMS"), code, "MOCK: demo code shown, real provider would SMS it");
  }

  @Override
  public VerificationResult verifyIdentity(User user, String fullName, String region) {
    if (fullName != null && fullName.startsWith("Review ")) {
      return new VerificationResult(
          VerificationDecision.PENDING,
          ref("ID-REVIEW"),
          null,
          "MOCK: routed to manual review by demo rule");
    }
    return new VerificationResult(
        VerificationDecision.PENDING, ref("ID"), null, "MOCK: pending, confirm to complete");
  }

  @Override
  public VerificationResult confirmIdentity(User user, String reference) {
    if (reference != null && reference.contains("REVIEW")) {
      return new VerificationResult(
          VerificationDecision.PENDING, reference, null, "MOCK: still under manual review");
    }
    return new VerificationResult(
        VerificationDecision.APPROVED, reference, null, "MOCK: identity approved (demo)");
  }

  private String ref(String kind) {
    return "MOCK-" + kind + "-" + sequence.getAndIncrement();
  }
}
