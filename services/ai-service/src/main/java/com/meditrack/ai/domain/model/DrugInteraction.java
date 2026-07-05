package com.meditrack.ai.domain.model;

/** A single drug-drug interaction surfaced by the clinical reasoning engine. */
public record DrugInteraction(
        String drugA,
        String drugB,
        Severity severity,
        String mechanism,
        String clinicalConsequence,
        String management
) {
}
