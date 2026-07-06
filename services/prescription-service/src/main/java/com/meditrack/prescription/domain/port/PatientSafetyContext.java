package com.meditrack.prescription.domain.port;

import java.util.List;

/**
 * Patient clinical context supplied to the drug-safety screen alongside the
 * prescription's own (proposed) medications.
 *
 * <p>TODO: enrich from patient-service — current active medications, recorded
 * allergies, age and sex. Until that integration lands, callers pass
 * {@link #empty()} and the screen covers interactions among the proposed
 * medications only.
 */
public record PatientSafetyContext(
        List<String> currentMedications,
        List<String> allergies
) {

    public PatientSafetyContext {
        currentMedications = currentMedications == null ? List.of() : List.copyOf(currentMedications);
        allergies = allergies == null ? List.of() : List.copyOf(allergies);
    }

    public static PatientSafetyContext empty() {
        return new PatientSafetyContext(List.of(), List.of());
    }
}
