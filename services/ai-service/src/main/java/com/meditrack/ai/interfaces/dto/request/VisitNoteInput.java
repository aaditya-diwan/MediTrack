package com.meditrack.ai.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;

/** One past visit note in a patient-history summary request. */
public record VisitNoteInput(
        String date,
        @NotBlank(message = "visit note text is required")
        String note
) {
}
