package com.meditrack.appointment.application.usecase;

import com.meditrack.appointment.interfaces.dto.request.BookAppointmentRequest;
import com.meditrack.appointment.interfaces.dto.response.AppointmentResponse;

public interface BookAppointmentUseCase {
    AppointmentResponse bookAppointment(BookAppointmentRequest request);
}
