package com.meditrack.ai.application.usecase;

import com.meditrack.ai.domain.model.SoapNote;
import com.meditrack.ai.domain.model.SoapNoteCommand;

/** Structure free-text consultation notes into a SOAP note. */
public interface GenerateSoapNoteUseCase {

    SoapNote generate(SoapNoteCommand command);
}
