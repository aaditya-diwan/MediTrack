package com.meditrack.appointment.interfaces.dto.response;

import com.meditrack.appointment.domain.model.AppointmentStatus;
import com.meditrack.appointment.domain.model.AppointmentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AppointmentResponse {
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
}
