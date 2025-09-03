package com.meditrack.patient.infrastructure.persistence.impl;

import com.meditrack.patient.domain.model.MedicalRecord;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.repository.MedicalRecordRepository;
import com.meditrack.patient.infrastructure.persistence.entity.MedicalRecordEntity;
import com.meditrack.patient.infrastructure.persistence.entity.PatientEntity;
import com.meditrack.patient.infrastructure.persistence.mapper.MedicalRecordMapper;
import com.meditrack.patient.infrastructure.persistence.repository.MedicalRecordJpaRepository;
import com.meditrack.patient.infrastructure.persistence.repository.PatientJpaRepository;
import lombok.RequiredArgsConstructor;
import com.meditrack.patient.application.exception.PatientNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MedicalRecordRepositoryImpl implements MedicalRecordRepository {

    private final MedicalRecordJpaRepository medicalRecordJpaRepository;
    private final PatientJpaRepository patientJpaRepository;

    @Override
    public MedicalRecord save(MedicalRecord medicalRecord) {
        // Find the associated patient entity. This assumes the patient already exists.
        PatientEntity patientEntity = patientJpaRepository.findById(medicalRecord.getPatientId().getId())
                .orElseThrow(() -> new PatientNotFoundException("Patient not found for ID: " + medicalRecord.getPatientId().getId()));

        MedicalRecordEntity medicalRecordEntity = MedicalRecordMapper.toEntity(medicalRecord, patientEntity);
        MedicalRecordEntity savedEntity = medicalRecordJpaRepository.save(medicalRecordEntity);
        return MedicalRecordMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MedicalRecord> findById(String recordId) {
        return medicalRecordJpaRepository.findById(UUID.fromString(recordId))
                .map(MedicalRecordMapper::toDomain);
    }

    @Override
    public List<MedicalRecord> findByPatientId(PatientId patientId) {
        return medicalRecordJpaRepository.findByPatientId(patientId.getId()).stream()
                .map(MedicalRecordMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String recordId) {
        medicalRecordJpaRepository.deleteById(UUID.fromString(recordId));
    }
}
