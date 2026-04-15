package com.meditrack.insurance_service.infrastructure.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.insurance_service.infrastructure.messaging.event.PatientCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes patient.created.v1 events from patient-service.
 *
 * Currently logs the event for audit/observability purposes.
 * Extend this consumer to auto-create a policy stub or trigger eligibility
 * verification when a new patient is registered.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientCreatedEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${spring.kafka.topics.patient-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPatientCreated(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            PatientCreatedEvent event = objectMapper.convertValue(record.value(), PatientCreatedEvent.class);

            if (!"patient.created.v1".equals(event.getEventType())) {
                // This topic may carry other patient events; skip non-create ones
                log.debug("Ignoring patient event [type={}, key={}]", event.getEventType(), record.key());
                ack.acknowledge();
                return;
            }

            log.info("Received patient-created event [patientId={}, mrn={}]",
                    event.getPatient() != null ? event.getPatient().getPatientId() : "unknown",
                    event.getPatient() != null ? event.getPatient().getMrn() : "unknown");

            // TODO: trigger eligibility pre-check or create policy stub for the new patient

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process patient-created event [key={}, offset={}]",
                    record.key(), record.offset(), e);
            // Do NOT acknowledge — let the error handler retry with exponential backoff
            throw new RuntimeException("Failed to process patient-created event", e);
        }
    }
}
