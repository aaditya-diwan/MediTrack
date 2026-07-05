package com.meditrack.ai.infrastructure.messaging;

import com.meditrack.ai.domain.model.SafetyAssessment;
import com.meditrack.ai.domain.model.SafetyCheckCommand;
import com.meditrack.ai.infrastructure.messaging.event.PrescriptionSafetyFlaggedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes {@code prescription.safety.flagged.v1}. Best-effort: a broker outage
 * is logged and swallowed so it never blocks or fails the clinician's request.
 * (Producer config uses {@code max.block.ms=5000} to fail fast when Kafka is down.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrescriptionSafetyEventProducer {

    public static final String TOPIC = "prescription.safety.flagged.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishFlagged(SafetyCheckCommand command, SafetyAssessment assessment) {
        PrescriptionSafetyFlaggedEvent event = PrescriptionSafetyFlaggedEvent.builder()
                .prescriptionId(command.prescriptionId())
                .patientId(command.patientId())
                .overallRisk(assessment.overallRisk().name())
                .interactionCount(assessment.interactions().size())
                .allergyConflictCount(assessment.allergyConflicts().size())
                .requiresPharmacistReview(assessment.requiresPharmacistReview())
                .summary(assessment.summary())
                .modelUsed(assessment.modelUsed())
                .occurredAt(Instant.now())
                .build();

        String key = command.prescriptionId() != null ? command.prescriptionId().toString() : "unassigned";
        try {
            kafkaTemplate.send(TOPIC, key, event);
            log.info("Published {} for prescription {} (risk={})", TOPIC, key, assessment.overallRisk());
        } catch (Exception ex) {
            log.warn("Failed to publish {} (continuing without it): {}", TOPIC, ex.getMessage());
        }
    }
}
