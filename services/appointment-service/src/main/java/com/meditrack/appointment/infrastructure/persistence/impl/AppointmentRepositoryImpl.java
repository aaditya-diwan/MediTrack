package com.meditrack.appointment.infrastructure.persistence.impl;

import com.meditrack.appointment.domain.model.Appointment;
import com.meditrack.appointment.domain.repository.AppointmentRepository;
import com.meditrack.appointment.infrastructure.persistence.mapper.AppointmentPersistenceMapper;
import com.meditrack.appointment.infrastructure.persistence.repository.JpaAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AppointmentRepositoryImpl implements AppointmentRepository {

    private final JpaAppointmentRepository jpaRepository;
    private final AppointmentPersistenceMapper mapper;

    @Override
    public Appointment save(Appointment appointment) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(appointment)));
    }

    @Override
    public Optional<Appointment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Appointment> findByPatientId(UUID patientId) {
        return jpaRepository.findByPatientId(patientId).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByDoctorId(UUID doctorId) {
        return jpaRepository.findByDoctorId(doctorId).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByDoctorIdAndScheduledAtBetween(UUID doctorId, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByDoctorIdAndScheduledAtBetween(doctorId, from, to).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByDoctorIdAndScheduledAt(UUID doctorId, LocalDateTime scheduledAt) {
        return jpaRepository.existsByDoctorIdAndScheduledAt(doctorId, scheduledAt);
    }
}
