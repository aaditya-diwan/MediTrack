package com.meditrack.appointment.domain.repository;

import com.meditrack.appointment.domain.model.Appointment;
import com.meditrack.appointment.domain.model.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(UUID id);
    List<Appointment> findByPatientId(UUID patientId);
    List<Appointment> findByDoctorId(UUID doctorId);
    List<Appointment> findByDoctorIdAndScheduledAtBetween(UUID doctorId, LocalDateTime from, LocalDateTime to);
    boolean existsByDoctorIdAndScheduledAt(UUID doctorId, LocalDateTime scheduledAt);
}
