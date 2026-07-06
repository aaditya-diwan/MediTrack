package com.meditrack.ai.interfaces.dto.response;

import com.meditrack.ai.domain.model.SoapNote;

import java.time.Instant;
import java.util.List;

/**
 * API response for a generated SOAP note. The disclaimer is set server-side —
 * the authoring clinician must review the note before filing it.
 */
public record SoapNoteResponse(
        String subjective,
        String objective,
        String assessment,
        String plan,
        List<String> assessmentProblems,
        String followUp,
        String modelUsed,
        String disclaimer,
        Instant generatedAt
) {

    private static final String DISCLAIMER =
            "AI-structured draft note derived solely from the supplied text. The authoring clinician must "
            + "review, correct, and approve it before it enters the medical record.";

    public static SoapNoteResponse from(SoapNote n) {
        return new SoapNoteResponse(
                n.subjective(),
                n.objective(),
                n.assessment(),
                n.plan(),
                n.assessmentProblems(),
                n.followUp(),
                n.modelUsed(),
                DISCLAIMER,
                Instant.now()
        );
    }
}
