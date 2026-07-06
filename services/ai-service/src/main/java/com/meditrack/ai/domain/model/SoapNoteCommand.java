package com.meditrack.ai.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Free-text consultation notes plus optional patient context, to be structured
 * into a SOAP note. Self-contained: the AI service persists none of it.
 */
public record SoapNoteCommand(
        String consultationNotes,
        Integer patientAgeYears,
        String patientSex,
        List<String> knownConditions,
        Map<String, String> vitals
) {
}
