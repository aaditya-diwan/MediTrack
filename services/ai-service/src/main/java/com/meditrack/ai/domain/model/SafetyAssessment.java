package com.meditrack.ai.domain.model;

import java.util.List;

/**
 * The result of a prescription safety screen.
 *
 * @param overallRisk             the highest severity across all findings
 * @param summary                 a one-paragraph clinical summary
 * @param recommendation          the recommended next action
 * @param requiresPharmacistReview whether a human pharmacist must verify before dispensing
 * @param interactions            drug-drug interactions found
 * @param allergyConflicts        medication/allergy conflicts found
 * @param modelUsed               the open-weight model that produced the assessment
 */
public record SafetyAssessment(
        Severity overallRisk,
        String summary,
        String recommendation,
        boolean requiresPharmacistReview,
        List<DrugInteraction> interactions,
        List<AllergyConflict> allergyConflicts,
        String modelUsed
) {
    /** True when the prescription should not proceed without human sign-off. */
    public boolean isFlagged() {
        return overallRisk.isHigh() || !allergyConflicts.isEmpty();
    }
}
