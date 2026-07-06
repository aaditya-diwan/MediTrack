package com.meditrack.ai.interfaces.dto.request;

import com.meditrack.ai.domain.model.SoapNoteCommand;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Self-contained SOAP-note generation request; the AI service persists none of it. */
public record SoapNoteRequest(
        @NotBlank(message = "consultation notes are required")
        String consultationNotes,
        Integer patientAgeYears,
        String patientSex,
        List<String> knownConditions,
        Map<String, String> vitals
) {

    public SoapNoteCommand toCommand() {
        return new SoapNoteCommand(
                consultationNotes,
                patientAgeYears,
                patientSex,
                Optional.ofNullable(knownConditions).orElse(List.of()),
                Optional.ofNullable(vitals).orElse(Map.of())
        );
    }
}
