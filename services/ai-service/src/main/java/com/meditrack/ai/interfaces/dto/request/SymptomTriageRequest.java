package com.meditrack.ai.interfaces.dto.request;

import com.meditrack.ai.domain.model.TriageCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Optional;

/** Self-contained symptom-triage request; the AI service persists none of it. */
public record SymptomTriageRequest(
        @NotBlank(message = "symptoms description is required")
        String symptoms,
        String duration,
        Integer patientAgeYears,
        String patientSex,
        List<String> knownConditions,
        List<String> currentMedications,
        List<String> knownAllergies
) {

    public TriageCommand toCommand() {
        return new TriageCommand(
                patientAgeYears,
                patientSex,
                symptoms,
                duration,
                Optional.ofNullable(knownConditions).orElse(List.of()),
                Optional.ofNullable(currentMedications).orElse(List.of()),
                Optional.ofNullable(knownAllergies).orElse(List.of())
        );
    }
}
