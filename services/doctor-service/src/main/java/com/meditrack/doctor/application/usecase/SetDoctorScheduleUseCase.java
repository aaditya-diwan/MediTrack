package com.meditrack.doctor.application.usecase;

import com.meditrack.doctor.interfaces.dto.request.SetScheduleRequest;
import com.meditrack.doctor.interfaces.dto.response.AvailabilitySlotResponse;

import java.util.List;
import java.util.UUID;

public interface SetDoctorScheduleUseCase {
    List<AvailabilitySlotResponse> setSchedule(UUID doctorId, SetScheduleRequest request);
}
