package com.meditrack.appointment.application.usecase;

import com.meditrack.appointment.interfaces.dto.response.AppointmentResponse;

import java.util.UUID;

public interface CancelAppointmentUseCase {
    AppointmentResponse cancelAppointment(UUID id);
}
