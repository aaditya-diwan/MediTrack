package com.meditrack.ai.domain.model;

/**
 * One suggested ICD-10 code.
 *
 * @param code        the ICD-10 code (e.g. "E11.9")
 * @param description the official code description
 * @param confidence  how well the notes support this code
 * @param rationale   which part of the notes supports the code
 */
public record IcdCodeSuggestion(
        String code,
        String description,
        IcdConfidence confidence,
        String rationale
) {
}
