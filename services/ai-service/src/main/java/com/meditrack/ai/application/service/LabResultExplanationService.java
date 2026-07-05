package com.meditrack.ai.application.service;

import com.meditrack.ai.application.usecase.ExplainLabResultUseCase;
import com.meditrack.ai.domain.model.LabResultExplanation;
import com.meditrack.ai.domain.model.LabResultExplanationCommand;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Explains a lab panel by delegating to the clinical reasoning port. Read-only:
 * no events, no state — the explanation is returned to the caller and nothing
 * is persisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabResultExplanationService implements ExplainLabResultUseCase {

    private final ClinicalReasoningPort reasoningPort;

    @Override
    public LabResultExplanation explain(LabResultExplanationCommand command) {
        int count = command.results() == null ? 0 : command.results().size();
        LabResultExplanation explanation = reasoningPort.explainLabResult(command);
        log.info("Lab-result explanation complete: {} result(s), urgency={}", count, explanation.urgency());
        return explanation;
    }
}
