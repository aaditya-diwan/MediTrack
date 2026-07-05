package com.meditrack.ai.interfaces.dto.request;

import com.meditrack.ai.domain.model.Medication;
import com.meditrack.ai.domain.model.SafetyCheckCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Self-contained prescription safety-check request. The caller supplies the full
 * clinical picture; the AI service persists none of it.
 */
public record PrescriptionSafetyRequest(
        @NotEmpty(message = "at least one medication is required")
        @Valid
        List<MedicationInput> medications,
        List<String> currentMedications,
        List<String> knownAllergies,
        Integer patientAgeYears,
        String patientSex,
        UUID prescriptionId,
        UUID patientId
) {

    public SafetyCheckCommand toCommand() {
        List<Medication> meds = Optional.ofNullable(medications).orElse(List.of())
                .stream()
                .map(m -> new Medication(m.name(), m.dosage(), m.route()))
                .toList();

        return new SafetyCheckCommand(
                meds,
                Optional.ofNullable(currentMedications).orElse(List.of()),
                Optional.ofNullable(knownAllergies).orElse(List.of()),
                patientAgeYears,
                patientSex,
                prescriptionId,
                patientId
        );
    }
}
