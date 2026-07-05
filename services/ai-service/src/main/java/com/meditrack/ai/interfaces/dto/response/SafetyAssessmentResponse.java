package com.meditrack.ai.interfaces.dto.response;

import com.meditrack.ai.domain.model.SafetyAssessment;

import java.time.Instant;
import java.util.List;

/**
 * API response for a prescription safety screen. Always carries an explicit
 * advisory disclaimer — this output supports, and never replaces, a licensed
 * clinician's or pharmacist's judgement.
 */
public record SafetyAssessmentResponse(
        String overallRisk,
        boolean requiresPharmacistReview,
        String summary,
        String recommendation,
        List<InteractionView> interactions,
        List<AllergyConflictView> allergyConflicts,
        String modelUsed,
        String disclaimer,
        Instant generatedAt
) {

    private static final String DISCLAIMER =
            "AI-generated clinical decision support. Advisory only — verify against an authoritative "
            + "drug-interaction reference and confirm with a licensed pharmacist or physician before dispensing.";

    public record InteractionView(String drugA, String drugB, String severity,
                                  String mechanism, String clinicalConsequence, String management) {
    }

    public record AllergyConflictView(String medication, String allergen, String severity, String note) {
    }

    public static SafetyAssessmentResponse from(SafetyAssessment a) {
        List<InteractionView> interactions = a.interactions().stream()
                .map(i -> new InteractionView(i.drugA(), i.drugB(), i.severity().name(),
                        i.mechanism(), i.clinicalConsequence(), i.management()))
                .toList();

        List<AllergyConflictView> conflicts = a.allergyConflicts().stream()
                .map(c -> new AllergyConflictView(c.medication(), c.allergen(), c.severity().name(), c.note()))
                .toList();

        return new SafetyAssessmentResponse(
                a.overallRisk().name(),
                a.requiresPharmacistReview(),
                a.summary(),
                a.recommendation(),
                interactions,
                conflicts,
                a.modelUsed(),
                DISCLAIMER,
                Instant.now()
        );
    }
}
