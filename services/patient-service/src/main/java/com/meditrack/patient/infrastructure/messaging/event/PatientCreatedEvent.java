package com.meditrack.patient.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published on the {@code patient-events} topic when a patient is registered.
 *
 * The envelope (eventId / eventType / timestamp / source / patient) matches what
 * insurance-service's PatientCreatedEventConsumer expects; consumers filter on
 * {@code eventType == "patient.created.v1"}.
 *
 * <p>Deliberately contains no SSN or other sensitive identifiers beyond the MRN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientCreatedEvent {

    private String eventId;
    private String eventType;
    private long timestamp;
    private String source;
    private PatientData patient;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientData {
        private String patientId;
        private String mrn;
        private String firstName;
        private String lastName;
        /** ISO-8601 date (yyyy-MM-dd). */
        private String dateOfBirth;
        private Instant createdAt;
    }
}
