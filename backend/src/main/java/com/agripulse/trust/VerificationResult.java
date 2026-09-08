package com.agripulse.trust;

/**
 * Outcome of one verification check. The reference is always labelled with its
 * provider (e.g. MOCK-…) so demo results can never pass as real verification.
 */
public record VerificationResult(
    VerificationDecision decision, String reference, String channelCode, String note) {}
