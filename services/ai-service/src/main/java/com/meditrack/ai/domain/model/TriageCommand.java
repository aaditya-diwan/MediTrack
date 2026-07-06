package com.meditrack.ai.domain.model;

import java.util.List;

/**
 * Clinical context for a symptom triage. Self-contained: the caller supplies
 * the full picture and the AI service persists none of it.
 */
public record TriageCommand(
        Integer patientAgeYears,
        String patientSex,
        String symptoms,
        String duration,
        List<String> knownConditions,
        List<String> currentMedications,
        List<String> knownAllergies
) {
}
