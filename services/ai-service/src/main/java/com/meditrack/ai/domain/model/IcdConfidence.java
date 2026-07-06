package com.meditrack.ai.domain.model;

/**
 * How confident the model is in an ICD-10 code suggestion. Lenient parsing:
 * an unrecognised label becomes {@link #LOW} — an unfamiliar value must never
 * overstate confidence in a billing/diagnosis code.
 */
public enum IcdConfidence {
    LOW,
    MODERATE,
    HIGH;

    public static IcdConfidence fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return LOW;
        }
        try {
            return IcdConfidence.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            // Conservative default — never overstate confidence in an unfamiliar label.
            return LOW;
        }
    }
}
