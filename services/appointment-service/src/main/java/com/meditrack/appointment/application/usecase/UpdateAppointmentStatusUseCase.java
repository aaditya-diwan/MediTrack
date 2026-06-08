package com.meditrack.appointment.application.usecase;

import com.meditrack.appointment.domain.model.AppointmentStatus;
import com.meditrack.appointment.interfaces.dto.response.AppointmentResponse;

import java.util.UUID;

public interface UpdateAppointmentStatusUseCase {
    AppointmentResponse updateStatus(UUID id, AppointmentStatus status);
}
