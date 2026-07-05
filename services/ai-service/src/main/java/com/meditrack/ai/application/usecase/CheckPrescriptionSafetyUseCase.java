package com.meditrack.ai.application.usecase;

import com.meditrack.ai.domain.model.SafetyAssessment;
import com.meditrack.ai.domain.model.SafetyCheckCommand;

/** Screen a proposed prescription for drug-drug interactions and allergy conflicts. */
public interface CheckPrescriptionSafetyUseCase {

    SafetyAssessment check(SafetyCheckCommand command);
}
