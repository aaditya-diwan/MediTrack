package com.meditrack.ai.domain.model;

/**
 * How quickly a patient presenting with symptoms should be seen, least to most
 * urgent. Lenient parsing: an unrecognised label from the model becomes
 * {@link #URGENT} rather than {@link #ROUTINE}, so an odd value never reads as
 * "no rush" for a patient who may need care now.
 */
public enum TriageUrgency {
    ROUTINE,
    SOON,
    URGENT,
    EMERGENCY;

    /** EMERGENCY means call emergency services / go to the ED immediately. */
    public boolean isEmergency() {
        return this == EMERGENCY;
    }

    public static TriageUrgency fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return URGENT;
        }
        try {
            return TriageUrgency.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            // Conservative default — never understate an unfamiliar label.
            return URGENT;
        }
    }
}
