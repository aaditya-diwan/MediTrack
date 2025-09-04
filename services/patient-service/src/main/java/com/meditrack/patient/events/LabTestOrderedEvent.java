package com.meditrack.patient.events;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LabTestOrderedEvent {

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