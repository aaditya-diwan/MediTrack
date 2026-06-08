package com.meditrack.appointment.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentBookedEvent {
    private UUID appointmentId;
    private UUID patientId;
    private UUID doctorId;
    private LocalDateTime scheduledAt;
    private String type;
    private Instant occurredAt;
}
