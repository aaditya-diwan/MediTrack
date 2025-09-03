package com.meditrack.patient.application.service;

import com.meditrack.patient.domain.model.MedicalRecord;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.repository.MedicalRecordRepository;
import com.meditrack.patient.interfaces.dto.request.CreateMedicalRecordRequest;
import com.meditrack.patient.interfaces.dto.request.UpdateMedicalRecordRequest;
import org.junit.jupiter.api.BeforeEach;
import com.meditrack.patient.application.exception.MedicalRecordNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MedicalRecordApplicationServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @InjectMocks
    private MedicalRecordApplicationService medicalRecordApplicationService;

    private MedicalRecord medicalRecord;
    private CreateMedicalRecordRequest createRequest;
    private UpdateMedicalRecordRequest updateRequest;

    @BeforeEach
    void setUp() {
        medicalRecord = new MedicalRecord();
        medicalRecord.setRecordId(UUID.randomUUID().toString());
        medicalRecord.setPatientId(PatientId.generate());
        medicalRecord.setDiagnosis("Flu");
        medicalRecord.setTreatment("Rest and fluids");
        medicalRecord.setDate(LocalDate.now());

        createRequest = new CreateMedicalRecordRequest();
        createRequest.setPatientId(UUID.randomUUID());
        createRequest.setDiagnosis("Cold");
        createRequest.setTreatment("Tea");
        createRequest.setDate(LocalDate.now());

        updateRequest = new UpdateMedicalRecordRequest();
        updateRequest.setDiagnosis("Pneumonia");
        updateRequest.setTreatment("Antibiotics");
        updateRequest.setDate(LocalDate.now().plusDays(5));
    }

    @Test
    void createMedicalRecord_shouldReturnMedicalRecordResponse() {
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenReturn(medicalRecord);

        var response = medicalRecordApplicationService.createMedicalRecord(createRequest);

        assertNotNull(response);
        assertEquals("Flu", response.getDiagnosis());
    }

    @Test
    void updateMedicalRecord_shouldReturnUpdatedMedicalRecordResponse() {
        when(medicalRecordRepository.findById(any(String.class))).thenReturn(Optional.of(medicalRecord));
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenReturn(medicalRecord);

        var response = medicalRecordApplicationService.updateMedicalRecord(medicalRecord.getRecordId(), updateRequest);

        assertNotNull(response);
        assertEquals("Pneumonia", response.getDiagnosis());
    }

    @Test
    void updateMedicalRecord_shouldThrowExceptionWhenNotFound() {
        when(medicalRecordRepository.findById(any(String.class))).thenReturn(Optional.empty());

        assertThrows(MedicalRecordNotFoundException.class, () -> medicalRecordApplicationService.updateMedicalRecord("nonexistent", updateRequest));
    }

    @Test
    void getMedicalRecordById_shouldReturnMedicalRecordResponse() {
        when(medicalRecordRepository.findById(any(String.class))).thenReturn(Optional.of(medicalRecord));

        var response = medicalRecordApplicationService.getMedicalRecordById(medicalRecord.getRecordId());

        assertNotNull(response);
        assertEquals("Flu", response.getDiagnosis());
    }

    @Test
    void getMedicalRecordById_shouldThrowExceptionWhenNotFound() {
        when(medicalRecordRepository.findById(any(String.class))).thenReturn(Optional.empty());

        assertThrows(MedicalRecordNotFoundException.class, () -> medicalRecordApplicationService.getMedicalRecordById("nonexistent"));
    }

    @Test
    void getMedicalRecordsByPatientId_shouldReturnListOfMedicalRecordResponses() {
        when(medicalRecordRepository.findByPatientId(any(PatientId.class))).thenReturn(List.of(medicalRecord));

        var response = medicalRecordApplicationService.getMedicalRecordsByPatientId(medicalRecord.getPatientId().getId());

        assertFalse(response.isEmpty());
        assertEquals("Flu", response.get(0).getDiagnosis());
    }

    @Test
    void deleteMedicalRecord_shouldCallRepositoryDeleteById() {
        doNothing().when(medicalRecordRepository).deleteById(any(String.class));

        medicalRecordApplicationService.deleteMedicalRecord(medicalRecord.getRecordId());

        verify(medicalRecordRepository, times(1)).deleteById(medicalRecord.getRecordId());
    }
}
