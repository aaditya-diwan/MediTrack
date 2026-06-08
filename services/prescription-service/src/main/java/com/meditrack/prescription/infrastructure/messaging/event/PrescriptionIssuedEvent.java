package com.meditrack.prescription.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrescriptionIssuedEvent {
    private UUID prescriptionId;
    private UUID patientId;
    private UUID doctorId;
    private UUID appointmentId;
    private Instant occurredAt;
}
