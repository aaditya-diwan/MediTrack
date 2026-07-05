package com.meditrack.ai.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;

/** One newly-prescribed medication in a safety-check request. */
public record MedicationInput(
        @NotBlank(message = "medication name is required")
        String name,
        String dosage,
        String route
) {
}
