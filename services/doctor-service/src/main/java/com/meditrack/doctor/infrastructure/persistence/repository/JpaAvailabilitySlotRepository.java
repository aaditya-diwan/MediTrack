package com.meditrack.doctor.infrastructure.persistence.repository;

import com.meditrack.doctor.infrastructure.persistence.entity.AvailabilitySlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaAvailabilitySlotRepository extends JpaRepository<AvailabilitySlotEntity, UUID> {
    List<AvailabilitySlotEntity> findByDoctorId(UUID doctorId);
    List<AvailabilitySlotEntity> findByDoctorIdAndDayOfWeek(UUID doctorId, String dayOfWeek);
    void deleteByDoctorId(UUID doctorId);
}
