package com.meditrack.ai.application.service;

import com.meditrack.ai.application.usecase.GenerateSoapNoteUseCase;
import com.meditrack.ai.domain.model.SoapNote;
import com.meditrack.ai.domain.model.SoapNoteCommand;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Structures free-text consultation notes into a SOAP note by delegating to the
 * clinical reasoning port. Read-only: nothing is persisted — the note goes back
 * to the clinician for review and filing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SoapNoteService implements GenerateSoapNoteUseCase {

    private final ClinicalReasoningPort reasoningPort;

    @Override
    public SoapNote generate(SoapNoteCommand command) {
        SoapNote note = reasoningPort.generateSoapNote(command);
        log.info("SOAP note generation complete: problems={}, followUp={}",
                note.assessmentProblems().size(), note.followUp() != null);
        return note;
    }
}
