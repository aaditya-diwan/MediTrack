package com.meditrack.ai.domain.model;

import java.util.List;

/**
 * A structured SOAP note derived strictly from the clinician's own free-text
 * notes — sections not documented in the input read "Not documented." rather
 * than being invented.
 *
 * @param subjective         what the patient reported
 * @param objective          examination findings and vitals
 * @param assessment         the clinician's assessment narrative
 * @param plan               the management plan
 * @param assessmentProblems the individual problems identified in the assessment
 * @param followUp           follow-up arrangement, if documented (nullable)
 * @param modelUsed          the open-weight model that produced this
 */
public record SoapNote(
        String subjective,
        String objective,
        String assessment,
        String plan,
        List<String> assessmentProblems,
        String followUp,
        String modelUsed
) {
}
