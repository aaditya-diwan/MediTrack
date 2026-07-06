package com.meditrack.ai.interfaces.dto.request;

import com.meditrack.ai.domain.model.IcdCodeSuggestionCommand;
import jakarta.validation.constraints.NotBlank;

/** Self-contained ICD-10 code suggestion request; the AI service persists none of it. */
public record IcdCodeSuggestionRequest(
        @NotBlank(message = "clinical notes are required")
        String clinicalNotes,
        String existingDiagnosis
) {

    public IcdCodeSuggestionCommand toCommand() {
        return new IcdCodeSuggestionCommand(clinicalNotes, existingDiagnosis);
    }
}
