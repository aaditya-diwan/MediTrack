package com.meditrack.appointment.interfaces.dto.request;

import com.meditrack.appointment.domain.model.AppointmentType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BookAppointmentRequest {
    @NotNull
    private UUID patientId;
    @NotNull
    private UUID doctorId;
    @NotNull
    private AppointmentType type;
    @NotNull @Future
    private LocalDateTime scheduledAt;
    private String reasonForVisit;
}
