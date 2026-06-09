package com.meditrack.patient.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Publishes patient domain events to Kafka. Every send runs on a separate thread ({@code @Async})
 * and is non-fatal: a broker outage neither blocks nor rolls back the caller's request
 * (at-most-once / graceful degradation), in line with the platform-wide publishing pattern.
 *
 * <p>Note: this producer is currently not wired into the patient command flow; the methods are
 * kept resilient-by-construction so that whenever they are invoked they degrade gracefully.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientEventProducer {

    private static final String PATIENT_CREATED_TOPIC = EventTopics.PATIENT_CREATED;
    private static final String PATIENT_UPDATED_TOPIC = EventTopics.PATIENT_UPDATED;
    private static final String PATIENT_DELETED_TOPIC = EventTopics.PATIENT_DELETED;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void sendPatientCreatedEvent(String patientId) {
        log.info("Publishing patient-created event [patientId={}, topic={}]", patientId, PATIENT_CREATED_TOPIC);
        send(PATIENT_CREATED_TOPIC, patientId, patientId, "patient-created", patientId);
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