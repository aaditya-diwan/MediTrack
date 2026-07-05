package com.meditrack.ai.domain.model;

/** Per-test explanation within a lab-result explanation. */
public record LabResultDetail(
        String testName,
        String interpretation,
        String explanation,
        String clinicalSignificance
) {
}
