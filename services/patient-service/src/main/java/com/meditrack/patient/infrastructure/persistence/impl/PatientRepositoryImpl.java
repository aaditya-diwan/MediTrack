package com.meditrack.patient.infrastructure.persistence.impl;

import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.MRN;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.domain.repository.PatientRepository;
import com.meditrack.patient.infrastructure.persistence.entity.PatientEntity;
import com.meditrack.patient.infrastructure.persistence.mapper.PatientMapper;
import com.meditrack.patient.infrastructure.persistence.repository.PatientJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PatientRepositoryImpl implements PatientRepository {

    private final PatientJpaRepository patientJpaRepository;

    @Override
    public Patient save(Patient patient) {
        PatientEntity patientEntity = PatientMapper.toEntity(patient);
        PatientEntity savedEntity = patientJpaRepository.save(patientEntity);
        return PatientMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Patient> findById(PatientId patientId) {
        return patientJpaRepository.findById(patientId.getId())
                .map(PatientMapper::toDomain);
    }

    @Override
    public void deleteById(PatientId patientId) {
        patientJpaRepository.deleteById(patientId.getId());
    }

    @Override
    public Optional<Patient> findByMrn(MRN mrn) {
        return patientJpaRepository.findByMrn(mrn.getValue())
                .map(PatientMapper::toDomain);
    }

    @Override
    public List<Patient> findByFirstName(String firstName) {
        return patientJpaRepository.findByFirstNameContainingIgnoreCase(firstName).stream()
                .map(PatientMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Patient> findByLastName(String lastName) {
        return patientJpaRepository.findByLastNameContainingIgnoreCase(lastName).stream()
                .map(PatientMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Patient> findBySsn(SSN ssn) {
        return patientJpaRepository.findBySsn(ssn.getValue())
                .map(PatientMapper::toDomain);
    }
}
