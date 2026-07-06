package com.meditrack.ai.application.usecase;

import com.meditrack.ai.domain.model.TriageAssessment;
import com.meditrack.ai.domain.model.TriageCommand;

/** Produce an advisory triage (urgency + specialty) for a symptom presentation. */
public interface TriageSymptomsUseCase {

    TriageAssessment triage(TriageCommand command);
}
