package com.meditrack.ai.interfaces.dto.response;

import com.meditrack.ai.domain.model.PatientHistorySummary;

import java.time.Instant;
import java.util.List;

/**
 * API response for a patient-history brief. The disclaimer is set server-side —
 * the brief supports, and never replaces, review of the full record.
 */
public record HistorySummaryResponse(
        List<String> keyConditions,
        List<String> activeMedications,
        List<String> criticalAllergies,
        List<String> recentAbnormalFindings,
        List<String> redFlags,
        String narrativeSummary,
        List<String> suggestedFollowUps,
        String modelUsed,
        String disclaimer,
        Instant generatedAt
) {

    private static final String DISCLAIMER =
            "AI-generated summary of the supplied record only. Advisory — it may omit relevant history; "
            + "consult the full medical record before clinical decisions.";

    public static HistorySummaryResponse from(PatientHistorySummary s) {
        return new HistorySummaryResponse(
                s.keyConditions(),
                s.activeMedications(),
                s.criticalAllergies(),
                s.recentAbnormalFindings(),
                s.redFlags(),
                s.narrativeSummary(),
                s.suggestedFollowUps(),
                s.modelUsed(),
                DISCLAIMER,
                Instant.now()
        );
    }
}
