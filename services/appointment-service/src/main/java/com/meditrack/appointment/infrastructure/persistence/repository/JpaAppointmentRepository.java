package com.meditrack.appointment.infrastructure.persistence.repository;

import com.meditrack.appointment.infrastructure.persistence.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaAppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {
    List<AppointmentEntity> findByPatientId(UUID patientId);
    List<AppointmentEntity> findByDoctorId(UUID doctorId);
    List<AppointmentEntity> findByDoctorIdAndScheduledAtBetween(UUID doctorId, LocalDateTime from, LocalDateTime to);
    boolean existsByDoctorIdAndScheduledAt(UUID doctorId, LocalDateTime scheduledAt);
}
