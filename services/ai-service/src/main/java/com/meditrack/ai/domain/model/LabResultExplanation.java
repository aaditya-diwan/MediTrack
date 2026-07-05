package com.meditrack.ai.domain.model;

import java.util.List;

/**
 * A plain-language explanation of a lab panel.
 *
 * @param overallSummary          clinician-facing summary of the panel
 * @param patientFriendlySummary  the same, in plain language for a patient
 * @param suggestedFollowUp       recommended next step (advisory)
 * @param urgency                 how quickly the findings should be acted on
 * @param results                 per-test detail
 * @param modelUsed               the open-weight model that produced this
 */
public record LabResultExplanation(
        String overallSummary,
        String patientFriendlySummary,
        String suggestedFollowUp,
        ClinicalUrgency urgency,
        List<LabResultDetail> results,
        String modelUsed
) {
}
