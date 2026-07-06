package com.meditrack.ai.interfaces.dto.response;

import com.meditrack.ai.domain.model.TriageAssessment;

import java.time.Instant;
import java.util.List;

/**
 * API response for a symptom triage. The disclaimer is set server-side and
 * never comes from the model — this output prioritises, it does not diagnose.
 */
public record SymptomTriageResponse(
        String urgency,
        boolean emergency,
        String recommendedSpecialty,
        List<String> redFlags,
        String rationale,
        String selfCareAdvice,
        String modelUsed,
        String disclaimer,
        Instant generatedAt
) {

    private static final String DISCLAIMER =
            "AI-generated triage guidance. Advisory only — this is not a diagnosis. If symptoms are severe "
            + "or worsening, seek emergency care immediately regardless of this assessment.";

    public static SymptomTriageResponse from(TriageAssessment a) {
        return new SymptomTriageResponse(
                a.urgency().name(),
                a.urgency().isEmergency(),
                a.recommendedSpecialty(),
                a.redFlags(),
                a.rationale(),
                a.selfCareAdvice(),
                a.modelUsed(),
                DISCLAIMER,
                Instant.now()
        );
    }
}
