package com.meditrack.patient.domain.repository;

import com.meditrack.patient.domain.model.MedicalRecord;
import com.meditrack.patient.domain.model.valueobjects.PatientId;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository {
    MedicalRecord save(MedicalRecord medicalRecord);
    Optional<MedicalRecord> findById(String recordId);
    List<MedicalRecord> findByPatientId(PatientId patientId);
    void deleteById(String recordId);
}
