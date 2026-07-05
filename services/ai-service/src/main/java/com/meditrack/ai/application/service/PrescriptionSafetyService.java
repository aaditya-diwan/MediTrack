package com.meditrack.ai.application.service;

import com.meditrack.ai.application.usecase.CheckPrescriptionSafetyUseCase;
import com.meditrack.ai.domain.model.SafetyAssessment;
import com.meditrack.ai.domain.model.SafetyCheckCommand;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import com.meditrack.ai.infrastructure.messaging.PrescriptionSafetyEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates a prescription safety screen: delegate the clinical judgement to
 * the reasoning port, then announce a flagged result on the event backbone so
 * downstream consumers (pharmacy queue, notifications, audit) can react.
 *
 * <p>Event publication is best-effort and never blocks the caller's answer —
 * a broker outage must not prevent a clinician from seeing the assessment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionSafetyService implements CheckPrescriptionSafetyUseCase {

    private final ClinicalReasoningPort reasoningPort;
    private final PrescriptionSafetyEventProducer eventProducer;

    @Override
    public SafetyAssessment check(SafetyCheckCommand command) {
        SafetyAssessment assessment = reasoningPort.assess(command);

        log.info("Prescription safety screen complete: risk={}, interactions={}, allergyConflicts={}",
                assessment.overallRisk(), assessment.interactions().size(), assessment.allergyConflicts().size());

        if (assessment.isFlagged()) {
            eventProducer.publishFlagged(command, assessment);
        }
        return assessment;
    }
}
