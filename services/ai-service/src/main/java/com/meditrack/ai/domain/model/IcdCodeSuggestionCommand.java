package com.meditrack.ai.domain.model;

/**
 * Clinical notes to derive ICD-10 code suggestions from, plus any diagnosis
 * the clinician has already written. Self-contained; nothing is persisted.
 */
public record IcdCodeSuggestionCommand(
        String clinicalNotes,
        String existingDiagnosis
) {
}
