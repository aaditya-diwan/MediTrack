package com.meditrack.patient.infrastructure.messaging;

import org.springframework.stereotype.Component;

@Component
public class PatientEventProducer {
    // This is a more realistic stub for a Kafka producer.
    // An actual implementation would use KafkaTemplate to send messages.
    public void sendPatientCreatedEvent(String patientId) {
        System.out.println("Simulating Kafka event: Sending patient created event for ID: " + patientId);
        // Simulate message sending delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
