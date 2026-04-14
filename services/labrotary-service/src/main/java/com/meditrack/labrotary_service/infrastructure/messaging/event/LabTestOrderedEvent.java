package com.meditrack.labrotary_service.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event received from the patient service when a lab test is ordered.
 * Mirrors the structure published by patient-service on topic {@link EventTopics#PATIENT_EVENTS_CONSUMER_TYPE}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabTestOrderedEvent {

    private UUID eventId;
    private String eventType;
    private long timestamp;
    private String source;
    private Order order;
    private PatientSnapshot patientSnapshot;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        private UUID orderId;
        private String patientId;
        private String doctorId;
        private String testCode;
        private String priority;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientSnapshot {
        private String mrn;
        private String firstName;
        private String lastName;
        private String dateOfBirth;
    }
}
