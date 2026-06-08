package com.meditrack.doctor.domain.repository;

import com.meditrack.doctor.domain.model.AvailabilitySlot;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface AvailabilitySlotRepository {
    AvailabilitySlot save(AvailabilitySlot slot);
    List<AvailabilitySlot> findByDoctorId(UUID doctorId);
    List<AvailabilitySlot> findByDoctorIdAndDayOfWeek(UUID doctorId, DayOfWeek dayOfWeek);
    void deleteByDoctorId(UUID doctorId);
}
