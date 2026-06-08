package com.meditrack.appointment.application.usecase;

import com.meditrack.appointment.interfaces.dto.response.AppointmentResponse;

import java.util.List;
import java.util.UUID;

public interface GetDoctorScheduleUseCase {
    List<AppointmentResponse> getDoctorSchedule(UUID doctorId);
}
