package com.meditrack.doctor.application.usecase;

import com.meditrack.doctor.interfaces.dto.response.AvailabilitySlotResponse;

import java.util.List;
import java.util.UUID;

public interface GetAvailableSlotsUseCase {
    List<AvailabilitySlotResponse> getSlots(UUID doctorId);
}
