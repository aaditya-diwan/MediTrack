package com.meditrack.ai.application.service;

import com.meditrack.ai.application.usecase.SummarizePatientHistoryUseCase;
import com.meditrack.ai.domain.model.HistorySummaryCommand;
import com.meditrack.ai.domain.model.PatientHistorySummary;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Distils a patient record into a pre-consultation brief by delegating to the
 * clinical reasoning port. Read-only: the caller supplies the record, the brief
 * is returned, and nothing is persisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientHistorySummaryService implements SummarizePatientHistoryUseCase {

    private final ClinicalReasoningPort reasoningPort;

    @Override
    public PatientHistorySummary summarize(HistorySummaryCommand command) {
        PatientHistorySummary summary = reasoningPort.summarizeHistory(command);
        log.info("Patient history summary complete: keyConditions={}, redFlags={}",
                summary.keyConditions().size(), summary.redFlags().size());
        return summary;
    }
}
