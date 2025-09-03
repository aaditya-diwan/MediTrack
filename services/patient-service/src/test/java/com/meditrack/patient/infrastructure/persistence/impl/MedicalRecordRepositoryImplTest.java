package com.meditrack.patient.infrastructure.persistence.impl;

import com.meditrack.patient.domain.model.MedicalRecord;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.infrastructure.persistence.entity.MedicalRecordEntity;
import com.meditrack.patient.infrastructure.persistence.entity.PatientEntity;
import com.meditrack.patient.infrastructure.persistence.mapper.MedicalRecordMapper;
import com.meditrack.patient.infrastructure.persistence.repository.MedicalRecordJpaRepository;
import com.meditrack.patient.infrastructure.persistence.repository.PatientJpaRepository;
import org.junit.jupiter.api.BeforeEach;
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
public class MedicalRecordRepositoryImplTest {

    @Mock
    private MedicalRecordJpaRepository medicalRecordJpaRepository;

    @Mock
    private PatientJpaRepository patientJpaRepository;

    @InjectMocks
    private MedicalRecordRepositoryImpl medicalRecordRepository;

    private MedicalRecord medicalRecord;
    private MedicalRecordEntity medicalRecordEntity;
    private PatientEntity patientEntity;

    @BeforeEach
    void setUp() {
        UUID patientUuid = UUID.randomUUID();
        UUID medicalRecordUuid = UUID.randomUUID();

        patientEntity = new PatientEntity();
        patientEntity.setId(patientUuid);

        medicalRecord = new MedicalRecord();
        medicalRecord.setRecordId(medicalRecordUuid.toString());
        medicalRecord.setPatientId(new PatientId(patientUuid));
        medicalRecord.setDiagnosis("Flu");
        medicalRecord.setTreatment("Rest and fluids");
        medicalRecord.setDate(LocalDate.now());

        medicalRecordEntity = new MedicalRecordEntity();
        medicalRecordEntity.setId(medicalRecordUuid);
        medicalRecordEntity.setPatient(patientEntity);
        medicalRecordEntity.setDiagnosis("Flu");
        medicalRecordEntity.setTreatment("Rest and fluids");
        medicalRecordEntity.setDate(LocalDate.now());
    }

    @Test
    void save_shouldReturnSavedMedicalRecord() {
        when(patientJpaRepository.findById(any(UUID.class))).thenReturn(Optional.of(patientEntity));
        when(medicalRecordJpaRepository.save(any(MedicalRecordEntity.class))).thenReturn(medicalRecordEntity);

        MedicalRecord savedMedicalRecord = medicalRecordRepository.save(medicalRecord);

        assertNotNull(savedMedicalRecord);
        assertEquals(medicalRecord.getDiagnosis(), savedMedicalRecord.getDiagnosis());
    }

    @Test
    void findById_shouldReturnMedicalRecord() {
        when(medicalRecordJpaRepository.findById(any(UUID.class))).thenReturn(Optional.of(medicalRecordEntity));

        Optional<MedicalRecord> foundMedicalRecord = medicalRecordRepository.findById(medicalRecord.getRecordId());

        assertTrue(foundMedicalRecord.isPresent());
        assertEquals(medicalRecord.getDiagnosis(), foundMedicalRecord.get().getDiagnosis());
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        when(medicalRecordJpaRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        Optional<MedicalRecord> foundMedicalRecord = medicalRecordRepository.findById(medicalRecord.getRecordId());

        assertFalse(foundMedicalRecord.isPresent());
    }

    @Test
    void findByPatientId_shouldReturnListOfMedicalRecords() {
        when(medicalRecordJpaRepository.findByPatientId(any(UUID.class))).thenReturn(List.of(medicalRecordEntity));

        List<MedicalRecord> foundMedicalRecords = medicalRecordRepository.findByPatientId(medicalRecord.getPatientId());

        assertFalse(foundMedicalRecords.isEmpty());
        assertEquals(medicalRecord.getDiagnosis(), foundMedicalRecords.get(0).getDiagnosis());
    }

    @Test
    void deleteById_shouldCallJpaRepositoryDeleteById() {
        doNothing().when(medicalRecordJpaRepository).deleteById(any(UUID.class));

        medicalRecordRepository.deleteById(medicalRecord.getRecordId());

        verify(medicalRecordJpaRepository, times(1)).deleteById(UUID.fromString(medicalRecord.getRecordId()));
    }
}
