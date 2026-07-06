package com.meditrack.patient.infrastructure.messaging;

import com.meditrack.patient.infrastructure.messaging.event.PatientCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Publishes patient domain events to Kafka. Every send runs on a separate thread ({@code @Async})
 * and is non-fatal: a broker outage neither blocks nor rolls back the caller's request
 * (at-most-once / graceful degradation), in line with the platform-wide publishing pattern.
 *
 * <p>The patient-created event is wired into the command flow: {@code PatientCommandService}
 * publishes a {@link PatientCreatedEvent} via the {@code ApplicationEventPublisher}, and
 * {@link #publishPatientCreated} relays it to Kafka only after the registration transaction
 * has committed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientEventProducer {

    private static final String PATIENT_UPDATED_TOPIC = EventTopics.PATIENT_UPDATED;
    private static final String PATIENT_DELETED_TOPIC = EventTopics.PATIENT_DELETED;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes the patient-created event onto the shared {@code patient-events} topic
     * (eventType {@code patient.created.v1} inside the envelope — the format
     * insurance-service's PatientCreatedEventConsumer filters on), only after the business
     * transaction has committed and on a separate thread, so that broker unavailability can
     * never roll back or block patient registration.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishPatientCreated(PatientCreatedEvent event) {
        String patientId = event.getPatient() != null ? event.getPatient().getPatientId() : null;
        log.info("Publishing patient-created event [patientId={}, topic={}]", patientId, EventTopics.PATIENT_EVENTS);
        send(EventTopics.PATIENT_EVENTS, patientId, event, "patient-created", patientId);
    }

    @Async
    public void sendPatientUpdatedEvent(String patientId) {
        log.info("Publishing patient-updated event [patientId={}, topic={}]", patientId, PATIENT_UPDATED_TOPIC);
        send(PATIENT_UPDATED_TOPIC, patientId, patientId, "patient-updated", patientId);
    }

    @Async
    public void sendPatientDeletedEvent(String patientId) {
        log.info("Publishing patient-deleted event [patientId={}, topic={}]", patientId, PATIENT_DELETED_TOPIC);
        send(PATIENT_DELETED_TOPIC, patientId, patientId, "patient-deleted", patientId);
    }

    /**
     * Publishes a structured {@link LabTestOrderedPayload} to the {@code patient-events} topic.
     * The lab-service consumes this to create a corresponding LabOrder.
     *
     * Call this from the use case / service layer when a physician orders a lab test for a patient.
     */
    @Async
    public void sendLabTestOrderedEvent(LabTestOrderedPayload payload) {
        UUID orderId = payload.getOrder().getOrderId();
        log.info("Publishing lab-test-ordered event [orderId={}, patientId={}, topic={}]",
                orderId, payload.getOrder().getPatientId(), EventTopics.PATIENT_EVENTS);
        send(EventTopics.PATIENT_EVENTS, payload.getOrder().getPatientId(), payload, "lab-test-ordered", orderId);
    }

    private void send(String topic, String key, Object value, String eventName, Object id) {
        try {
            kafkaTemplate.send(topic, key, value)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} event [id={}]: {}", eventName, id, ex.getMessage());
                        } else {
                            log.debug("{} event published successfully [id={}]", eventName, id);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing {} event [id={}]: {}", eventName, id, e.getMessage());
        }
    }
}