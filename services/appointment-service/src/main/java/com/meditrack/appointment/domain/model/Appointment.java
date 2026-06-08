package com.meditrack.appointment.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class Appointment {
    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private AppointmentStatus status;
    private AppointmentType type;
    private String reasonForVisit;
    private String notes;
    private LocalDateTime scheduledAt;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
