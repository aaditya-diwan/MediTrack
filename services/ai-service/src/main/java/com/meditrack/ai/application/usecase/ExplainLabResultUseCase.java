package com.meditrack.ai.application.usecase;

import com.meditrack.ai.domain.model.LabResultExplanation;
import com.meditrack.ai.domain.model.LabResultExplanationCommand;

/** Produce a clinician- and patient-facing explanation of a lab panel. */
public interface ExplainLabResultUseCase {

    LabResultExplanation explain(LabResultExplanationCommand command);
}
