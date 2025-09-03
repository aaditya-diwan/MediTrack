package com.meditrack.patient.infrastructure.persistence.impl;

import com.meditrack.patient.domain.model.ContactInfo;
import com.meditrack.patient.domain.model.Insurance;
import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.MRN;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.infrastructure.persistence.entity.ContactInfoEntity;
import com.meditrack.patient.infrastructure.persistence.entity.PatientEntity;
import com.meditrack.patient.infrastructure.persistence.mapper.PatientMapper;
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
public class PatientRepositoryImplTest {

    @Mock
    private PatientJpaRepository patientJpaRepository;

    @InjectMocks
    private PatientRepositoryImpl patientRepository;

    private Patient patient;
    private PatientEntity patientEntity;

    @BeforeEach
    void setUp() {
        UUID patientUuid = UUID.randomUUID();
        patient = new Patient();
        patient.setId(new PatientId(patientUuid));
        patient.setMrn(new MRN("MRN123"));
        patient.setSsn(new SSN("SSN123"));
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setContactInfo(new ContactInfo());
        patient.setInsurance(new Insurance());

        patientEntity = new PatientEntity();
        patientEntity.setId(patientUuid);
        patientEntity.setMrn("MRN123");
        patientEntity.setSsn("SSN123");
        patientEntity.setFirstName("John");
        patientEntity.setLastName("Doe");
        patientEntity.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patientEntity.setContactInfo(new ContactInfoEntity());
        patientEntity.setInsuranceProvider("ProviderA");
        patientEntity.setInsurancePolicyNumber("Policy123");
    }

    @Test
    void save_shouldReturnSavedPatient() {
        when(patientJpaRepository.save(any(PatientEntity.class))).thenReturn(patientEntity);

        Patient savedPatient = patientRepository.save(patient);

        assertNotNull(savedPatient);
        assertEquals(patient.getFirstName(), savedPatient.getFirstName());
    }

    @Test
    void findById_shouldReturnPatient() {
        when(patientJpaRepository.findById(any(UUID.class))).thenReturn(Optional.of(patientEntity));

        Optional<Patient> foundPatient = patientRepository.findById(patient.getId());

        assertTrue(foundPatient.isPresent());
        assertEquals(patient.getFirstName(), foundPatient.get().getFirstName());
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        when(patientJpaRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        Optional<Patient> foundPatient = patientRepository.findById(patient.getId());

        assertFalse(foundPatient.isPresent());
    }

    @Test
    void deleteById_shouldCallJpaRepositoryDeleteById() {
        doNothing().when(patientJpaRepository).deleteById(any(UUID.class));

        patientRepository.deleteById(patient.getId());

        verify(patientJpaRepository, times(1)).deleteById(patient.getId().getId());
    }

    @Test
    void findByMrn_shouldReturnPatient() {
        when(patientJpaRepository.findByMrn(any(String.class))).thenReturn(Optional.of(patientEntity));

        Optional<Patient> foundPatient = patientRepository.findByMrn(patient.getMrn());

        assertTrue(foundPatient.isPresent());
        assertEquals(patient.getMrn().getValue(), foundPatient.get().getMrn().getValue());
    }

    @Test
    void findByMrn_shouldReturnEmptyWhenNotFound() {
        when(patientJpaRepository.findByMrn(any(String.class))).thenReturn(Optional.empty());

        Optional<Patient> foundPatient = patientRepository.findByMrn(patient.getMrn());

        assertFalse(foundPatient.isPresent());
    }

    @Test
    void findByFirstName_shouldReturnListOfPatients() {
        when(patientJpaRepository.findByFirstNameContainingIgnoreCase(any(String.class))).thenReturn(List.of(patientEntity));

        List<Patient> foundPatients = patientRepository.findByFirstName(patient.getFirstName());

        assertFalse(foundPatients.isEmpty());
        assertEquals(patient.getFirstName(), foundPatients.get(0).getFirstName());
    }

    @Test
    void findByLastName_shouldReturnListOfPatients() {
        when(patientJpaRepository.findByLastNameContainingIgnoreCase(any(String.class))).thenReturn(List.of(patientEntity));

        List<Patient> foundPatients = patientRepository.findByLastName(patient.getLastName());

        assertFalse(foundPatients.isEmpty());
        assertEquals(patient.getLastName(), foundPatients.get(0).getLastName());
    }

    @Test
    void findBySsn_shouldReturnPatient() {
        when(patientJpaRepository.findBySsn(any(String.class))).thenReturn(Optional.of(patientEntity));

        Optional<Patient> foundPatient = patientRepository.findBySsn(patient.getSsn());

        assertTrue(foundPatient.isPresent());
        assertEquals(patient.getSsn().getValue(), foundPatient.get().getSsn().getValue());
    }
}
