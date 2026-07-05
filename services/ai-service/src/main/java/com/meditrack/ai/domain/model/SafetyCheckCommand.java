package com.meditrack.ai.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * The clinical context handed to the reasoning engine for a prescription
 * safety screen. Self-contained: the caller supplies the full picture, so the
 * AI service never has to reach back into other services (and never persists it).
 *
 * @param newMedications     medications being newly prescribed (screened against each other and the rest)
 * @param currentMedications names of medications the patient is already taking
 * @param knownAllergies     documented allergies / adverse reactions (e.g. "penicillin", "sulfa")
 * @param patientAgeYears    optional — informs age-sensitive interactions
 * @param patientSex         optional — informs sex-sensitive interactions
 * @param prescriptionId     optional correlation id (echoed onto the flagged event)
 * @param patientId          optional correlation id (echoed onto the flagged event)
 */
public record SafetyCheckCommand(
        List<Medication> newMedications,
        List<String> currentMedications,
        List<String> knownAllergies,
        Integer patientAgeYears,
        String patientSex,
        UUID prescriptionId,
        UUID patientId
) {
}
