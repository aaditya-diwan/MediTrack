package com.meditrack.ai.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;

/** One recent lab result in a patient-history summary request. */
public record HistoryLabResultInput(
        @NotBlank(message = "lab result name is required")
        String name,
        String value,
        String flag
) {
}
