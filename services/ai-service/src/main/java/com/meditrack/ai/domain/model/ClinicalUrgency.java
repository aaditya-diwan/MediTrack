package com.meditrack.ai.domain.model;

/**
 * How quickly a lab-result finding should be acted on, least to most urgent.
 * Lenient parsing: an unrecognised label from the model becomes {@link #MONITOR}
 * rather than {@link #ROUTINE}, so an odd value never reads as "nothing to do".
 */
public enum ClinicalUrgency {
    ROUTINE,
    MONITOR,
    URGENT,
    CRITICAL;

    public boolean isActionable() {
        return this == URGENT || this == CRITICAL;
    }

    public static ClinicalUrgency fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return ROUTINE;
        }
        try {
            return ClinicalUrgency.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MONITOR;
        }
    }
}
