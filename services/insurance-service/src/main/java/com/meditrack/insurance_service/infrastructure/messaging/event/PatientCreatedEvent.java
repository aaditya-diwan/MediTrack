package com.meditrack.insurance_service.infrastructure.messaging.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event received from patient-service when a patient is created.
 * Maps to the payload published on the "patient.created.v1" topic.
 */
@Data
@NoArgsConstructor
public class PatientCreatedEvent {

    private String eventId;
    private String eventType;
    private long timestamp;
    private String source;
    private PatientData patient;

    @Data
    @NoArgsConstructor
    public static class PatientData {
        private String patientId;
        private String mrn;
        private String firstName;
        private String lastName;
        private String dateOfBirth;
    }
}
