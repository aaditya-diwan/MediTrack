package com.meditrack.doctor.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCreatedEvent {
    private UUID doctorId;
    private String employeeId;
    private String fullName;
    private String specialization;
    private Instant occurredAt;
}
