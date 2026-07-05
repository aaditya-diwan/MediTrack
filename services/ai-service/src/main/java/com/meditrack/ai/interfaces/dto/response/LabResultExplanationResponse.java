package com.meditrack.ai.interfaces.dto.response;

import com.meditrack.ai.domain.model.LabResultExplanation;

import java.time.Instant;
import java.util.List;

/**
 * API response for a lab-result explanation. Carries an explicit advisory
 * disclaimer — this output supports, and never replaces, a clinician's reading
 * of the results.
 */
public record LabResultExplanationResponse(
        String urgency,
        String overallSummary,
        String patientFriendlySummary,
        String suggestedFollowUp,
        List<ResultView> results,
        String modelUsed,
        String disclaimer,
        Instant generatedAt
) {

    private static final String DISCLAIMER =
            "AI-generated explanation for clinical support. Advisory only — interpret alongside the full "
            + "clinical picture and confirm with a licensed clinician before acting.";

    public record ResultView(String testName, String interpretation,
                             String explanation, String clinicalSignificance) {
    }

    public static LabResultExplanationResponse from(LabResultExplanation e) {
        List<ResultView> results = e.results().stream()
                .map(r -> new ResultView(r.testName(), r.interpretation(),
                        r.explanation(), r.clinicalSignificance()))
                .toList();

        return new LabResultExplanationResponse(
                e.urgency().name(),
                e.overallSummary(),
                e.patientFriendlySummary(),
                e.suggestedFollowUp(),
                results,
                e.modelUsed(),
                DISCLAIMER,
                Instant.now()
        );
    }
}
