package com.meditrack.prescription.domain.port;

import java.util.List;

/**
 * Domain-level outcome of a drug-safety screen.
 *
 * @param checked                  true when the AI screen actually ran; false when the
 *                                 service was unreachable / misconfigured (fail-open)
 * @param highestSeverity          highest severity reported by the screen
 *                                 (NONE / MINOR / MODERATE / MAJOR / CONTRAINDICATED),
 *                                 null when {@code checked} is false
 * @param summary                  human-readable summary of the assessment
 * @param requiresPharmacistReview whether the screen recommends pharmacist sign-off
 * @param findings                 individual interaction / allergy findings
 */
public record SafetyScreenResult(
        boolean checked,
        String highestSeverity,
        String summary,
        boolean requiresPharmacistReview,
        List<SafetyFinding> findings
) {

    /** Severities that block issuance unless the doctor explicitly overrides. */
    private static final List<String> BLOCKING_SEVERITIES = List.of("MAJOR", "SEVERE", "CONTRAINDICATED");

    public record SafetyFinding(String type, String severity, String description) {
    }

    public SafetyScreenResult {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    /** Result used when the screen could not be performed (fail-open path). */
    public static SafetyScreenResult notChecked(String reason) {
        return new SafetyScreenResult(false, null,
                "AI safety screen unavailable: " + reason, false, List.of());
    }

    /** True when the reported severity warrants blocking issuance. */
    public boolean isBlocking() {
        return checked && highestSeverity != null
                && BLOCKING_SEVERITIES.contains(highestSeverity.trim().toUpperCase());
    }
}
