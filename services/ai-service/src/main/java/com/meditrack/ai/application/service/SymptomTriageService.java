package com.meditrack.ai.application.service;

import com.meditrack.ai.application.usecase.TriageSymptomsUseCase;
import com.meditrack.ai.domain.model.TriageAssessment;
import com.meditrack.ai.domain.model.TriageCommand;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Triages a symptom presentation by delegating to the clinical reasoning port.
 * Read-only: no events, no state — the assessment is returned to the caller and
 * nothing is persisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomTriageService implements TriageSymptomsUseCase {

    private final ClinicalReasoningPort reasoningPort;

    @Override
    public TriageAssessment triage(TriageCommand command) {
        TriageAssessment assessment = reasoningPort.triage(command);
        log.info("Symptom triage complete: urgency={}, redFlags={}",
                assessment.urgency(), assessment.redFlags().size());
        return assessment;
    }
}
