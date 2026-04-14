package com.meditrack.patient.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientEventProducer {

    private static final String PATIENT_CREATED_TOPIC = EventTopics.PATIENT_CREATED;
    private static final String PATIENT_UPDATED_TOPIC = EventTopics.PATIENT_UPDATED;
    private static final String PATIENT_DELETED_TOPIC = EventTopics.PATIENT_DELETED;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPatientCreatedEvent(String patientId) {
        log.info("Publishing patient-created event [patientId={}, topic={}]", patientId, PATIENT_CREATED_TOPIC);
        try {
            kafkaTemplate.send(PATIENT_CREATED_TOPIC, patientId, patientId);
            log.debug("Patient-created event published successfully [patientId={}]", patientId);
        } catch (Exception e) {
            log.error("Failed to publish patient-created event [patientId={}]", patientId, e);
        }
    }

    public void sendPatientUpdatedEvent(String patientId) {
        log.info("Publishing patient-updated event [patientId={}, topic={}]", patientId, PATIENT_UPDATED_TOPIC);
        try {
            kafkaTemplate.send(PATIENT_UPDATED_TOPIC, patientId, patientId);
            log.debug("Patient-updated event published successfully [patientId={}]", patientId);
        } catch (Exception e) {
            log.error("Failed to publish patient-updated event [patientId={}]", patientId, e);
        }
    }

    public void sendPatientDeletedEvent(String patientId) {
        log.info("Publishing patient-deleted event [patientId={}, topic={}]", patientId, PATIENT_DELETED_TOPIC);
        try {
            kafkaTemplate.send(PATIENT_DELETED_TOPIC, patientId, patientId);
            log.debug("Patient-deleted event published successfully [patientId={}]", patientId);
        } catch (Exception e) {
            log.error("Failed to publish patient-deleted event [patientId={}]", patientId, e);
        }
    }
}