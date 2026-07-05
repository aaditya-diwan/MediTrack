package com.meditrack.ai.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;

/** One lab measurement in a lab-result explanation request. */
public record LabValueInput(
        @NotBlank(message = "test name is required")
        String testName,
        String value,
        String unit,
        String referenceRange,
        String flag
) {
}
