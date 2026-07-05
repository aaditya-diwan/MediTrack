package com.meditrack.ai.domain.model;

/** A conflict between a proposed medication and a documented patient allergy. */
public record AllergyConflict(
        String medication,
        String allergen,
        Severity severity,
        String note
) {
}
