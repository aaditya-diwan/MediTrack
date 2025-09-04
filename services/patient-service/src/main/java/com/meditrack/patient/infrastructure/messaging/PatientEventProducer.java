package com.meditrack.patient.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientEventProducer {
    // This is a more realistic stub for a Kafka producer.
    // An actual implementation would use KafkaTemplate to send messages.

    private static final String PATIENT_CREATED_TOPIC = "patient.created.v1";
    private final KafkaTemplate<String, Object> kafkaTemplate;
    public void sendPatientCreatedEvent(String patientId) {
        System.out.println("Simulating Kafka event: Sending patient created event for ID: " + patientId);
        // Simulate message sending delay
        try {
            kafkaTemplate.send(PATIENT_CREATED_TOPIC, patientId, patientId);
        } catch (Exception e) {
            System.out.println("Error sending kafka message");
        }
        System.out.println("Patient created event sent.");
    }

    public void sendPatientUpdatedEvent(String patientId) {
        System.out.println("Simulating Kafka event: Sending patient updated event for ID: " + patientId);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Patient updated event sent.");
    }

    public void sendPatientDeletedEvent(String patientId) {
        System.out.println("Simulating Kafka event: Sending patient deleted event for ID: " + patientId);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Patient deleted event sent.");
    }

    
}