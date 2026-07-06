package com.meditrack.ai.domain.model;

/** One past visit note supplied for a patient-history summary. */
public record VisitNote(
        String date,
        String note
) {
}
