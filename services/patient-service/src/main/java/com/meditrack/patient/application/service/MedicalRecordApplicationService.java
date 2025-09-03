package com.meditrack.patient.application.service;

import com.meditrack.patient.application.mapper.ApplicationMedicalRecordMapper;
import com.meditrack.patient.domain.model.MedicalRecord;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.repository.MedicalRecordRepository;
import com.meditrack.patient.interfaces.dto.request.CreateMedicalRecordRequest;
import com.meditrack.patient.interfaces.dto.request.UpdateMedicalRecordRequest;
import com.meditrack.patient.interfaces.dto.response.MedicalRecordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.meditrack.patient.application.exception.MedicalRecordNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalRecordApplicationService {

    private final MedicalRecordRepository medicalRecordRepository;

    public MedicalRecordResponse createMedicalRecord(CreateMedicalRecordRequest request) {
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setPatientId(new PatientId(request.getPatientId()));
        medicalRecord.setDiagnosis(request.getDiagnosis());
        medicalRecord.setTreatment(request.getTreatment());
        medicalRecord.setDate(request.getDate());
        MedicalRecord savedMedicalRecord = medicalRecordRepository.save(medicalRecord);
        return ApplicationMedicalRecordMapper.toResponse(savedMedicalRecord);
    }

    public MedicalRecordResponse updateMedicalRecord(String recordId, UpdateMedicalRecordRequest request) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException("Medical record not found"));
        medicalRecord.setDiagnosis(request.getDiagnosis());
        medicalRecord.setTreatment(request.getTreatment());
        medicalRecord.setDate(request.getDate());
        MedicalRecord updatedMedicalRecord = medicalRecordRepository.save(medicalRecord);
        return ApplicationMedicalRecordMapper.toResponse(updatedMedicalRecord);
    }

    @Transactional(readOnly = true)
    public MedicalRecordResponse getMedicalRecordById(String recordId) {
        return medicalRecordRepository.findById(recordId)
                .map(ApplicationMedicalRecordMapper::toResponse)
                .orElseThrow(() -> new MedicalRecordNotFoundException("Medical record not found"));
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getMedicalRecordsByPatientId(UUID patientId) {
        return medicalRecordRepository.findByPatientId(new PatientId(patientId)).stream()
                .map(ApplicationMedicalRecordMapper::toResponse)
                .collect(Collectors.toList());
    }

    public void deleteMedicalRecord(String recordId) {
        medicalRecordRepository.deleteById(recordId);
    }
}
