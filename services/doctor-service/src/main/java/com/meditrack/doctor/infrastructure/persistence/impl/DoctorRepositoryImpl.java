package com.meditrack.doctor.infrastructure.persistence.impl;

import com.meditrack.doctor.domain.model.Doctor;
import com.meditrack.doctor.domain.model.Specialization;
import com.meditrack.doctor.domain.repository.DoctorRepository;
import com.meditrack.doctor.infrastructure.persistence.mapper.DoctorPersistenceMapper;
import com.meditrack.doctor.infrastructure.persistence.repository.JpaDoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DoctorRepositoryImpl implements DoctorRepository {

    private final JpaDoctorRepository jpaRepository;
    private final DoctorPersistenceMapper mapper;

    @Override
    public Doctor save(Doctor doctor) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(doctor)));
    }

    @Override
    public Optional<Doctor> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Doctor> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<Doctor> findByEmployeeId(String employeeId) {
        return jpaRepository.findByEmployeeId(employeeId).map(mapper::toDomain);
    }

    @Override
    public List<Doctor> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Doctor> findBySpecialization(Specialization specialization) {
        return jpaRepository.findBySpecialization(specialization.name()).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Doctor> findAllActive() {
        return jpaRepository.findByActiveTrue().stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmployeeId(String employeeId) {
        return jpaRepository.existsByEmployeeId(employeeId);
    }
}
