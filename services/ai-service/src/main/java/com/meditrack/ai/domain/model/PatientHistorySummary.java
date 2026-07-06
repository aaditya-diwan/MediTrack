package com.meditrack.ai.domain.model;

import java.util.List;

/**
 * A structured pre-consultation brief distilled from a patient's record.
 *
 * @param keyConditions          the conditions that matter most right now
 * @param activeMedications      medications the patient is currently on
 * @param criticalAllergies      allergies a prescriber must not miss
 * @param recentAbnormalFindings abnormal labs / findings from the supplied data
 * @param redFlags               anything that needs attention before the visit
 * @param narrativeSummary       a short clinician-facing narrative
 * @param suggestedFollowUps     advisory follow-up actions
 * @param modelUsed              the open-weight model that produced this
 */
public record PatientHistorySummary(
        List<String> keyConditions,
        List<String> activeMedications,
        List<String> criticalAllergies,
        List<String> recentAbnormalFindings,
        List<String> redFlags,
        String narrativeSummary,
        List<String> suggestedFollowUps,
        String modelUsed
) {
}
