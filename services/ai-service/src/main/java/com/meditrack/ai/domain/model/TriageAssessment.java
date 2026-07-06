package com.meditrack.ai.domain.model;

import java.util.List;

/**
 * Advisory triage of a symptom presentation.
 *
 * @param urgency              how quickly the patient should be seen
 * @param recommendedSpecialty the specialty best placed to see the patient
 * @param redFlags             red-flag findings that drove the urgency
 * @param rationale            why the model chose this urgency
 * @param selfCareAdvice       optional self-care guidance (null when not appropriate)
 * @param modelUsed            the open-weight model that produced this
 */
public record TriageAssessment(
        TriageUrgency urgency,
        String recommendedSpecialty,
        List<String> redFlags,
        String rationale,
        String selfCareAdvice,
        String modelUsed
) {
}
