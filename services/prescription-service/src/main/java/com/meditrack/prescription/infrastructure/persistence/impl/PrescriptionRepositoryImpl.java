package com.meditrack.prescription.infrastructure.persistence.impl;

import com.meditrack.prescription.domain.model.Prescription;
import com.meditrack.prescription.domain.repository.PrescriptionRepository;
import com.meditrack.prescription.infrastructure.persistence.mapper.PrescriptionPersistenceMapper;
import com.meditrack.prescription.infrastructure.persistence.repository.JpaPrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PrescriptionRepositoryImpl implements PrescriptionRepository {

    private final JpaPrescriptionRepository jpaRepository;
    private final PrescriptionPersistenceMapper mapper;

    @Override
    public Prescription save(Prescription prescription) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(prescription)));
    }

    @Override
    public Optional<Prescription> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Prescription> findByPatientId(UUID patientId) {
        return jpaRepository.findByPatientId(patientId).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Prescription> findByDoctorId(UUID doctorId) {
        return jpaRepository.findByDoctorId(doctorId).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Prescription> findByAppointmentId(UUID appointmentId) {
        return jpaRepository.findByAppointmentId(appointmentId).stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
