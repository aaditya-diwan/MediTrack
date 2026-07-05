package com.meditrack.ai.domain.model;

/**
 * Standard clinical drug-interaction / allergy severity tiers, ordered from
 * least to most dangerous. Parsing is deliberately lenient: an unrecognised
 * value from the model is treated as {@link #MODERATE} rather than dropped, so
 * an unexpected label never silently downgrades a real risk to "none".
 */
public enum Severity {
    NONE,
    MINOR,
    MODERATE,
    MAJOR,
    CONTRAINDICATED;

    /** MAJOR and CONTRAINDICATED warrant a hard stop / pharmacist review. */
    public boolean isHigh() {
        return this == MAJOR || this == CONTRAINDICATED;
    }

    public static Severity fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        try {
            return Severity.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            // Conservative default — never understate an unfamiliar label.
            return MODERATE;
        }
    }
}
