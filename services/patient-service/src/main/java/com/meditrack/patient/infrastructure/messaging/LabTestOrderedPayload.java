package com.meditrack.patient.infrastructure.messaging;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Outbound event payload published to {@link EventTopics#PATIENT_EVENTS} when a physician orders
 * a lab test for a patient. The lab-service consumes this and creates a corresponding LabOrder.
 *
 * Must stay in sync with {@code LabTestOrderedEvent} in the lab-service.
 */
@Data
@Builder
public class LabTestOrderedPayload {

    private UUID eventId;
    private String eventType;
    private long timestamp;
    private String source;
    private Order order;
    private PatientSnapshot patientSnapshot;

    @Data
    @Builder
    public static class Order {
        private UUID orderId;
        private String patientId;
        private String doctorId;
        private String testCode;
        private String priority;
        private String notes;
    }

    @Data
    @Builder
    public static class PatientSnapshot {
        private String mrn;
        private String firstName;
        private String lastName;
        private String dateOfBirth;
    }
}
