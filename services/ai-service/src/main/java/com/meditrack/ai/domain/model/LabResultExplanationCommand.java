package com.meditrack.ai.domain.model;

import java.util.List;

/**
 * Clinical context for explaining a set of lab results. Self-contained: the
 * caller supplies the full panel and optional patient context; the AI service
 * persists none of it.
 */
public record LabResultExplanationCommand(
        List<LabValue> results,
        Integer patientAgeYears,
        String patientSex,
        String context
) {
}
