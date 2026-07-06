package com.meditrack.ai.domain.model;

import java.util.List;

/**
 * The full picture supplied for a patient-history summary: demographics,
 * problem/medication/allergy lists, recent lab results and past visit notes.
 * Self-contained: the AI service persists none of it.
 */
public record HistorySummaryCommand(
        Integer patientAgeYears,
        String patientSex,
        List<String> conditions,
        List<String> medications,
        List<String> allergies,
        List<LabValue> recentLabResults,
        List<VisitNote> pastVisits
) {
}
