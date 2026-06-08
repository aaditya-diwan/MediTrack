package com.meditrack.appointment.application.usecase;

import com.meditrack.appointment.interfaces.dto.response.AppointmentResponse;

import java.util.UUID;

public interface GetAppointmentUseCase {
    AppointmentResponse getAppointmentById(UUID id);
}
