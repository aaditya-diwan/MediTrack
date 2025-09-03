package com.meditrack.patient.application.service;

import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.MRN;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.domain.repository.PatientRepository;
import com.meditrack.patient.application.exception.PatientNotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PatientQueryServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientQueryService patientQueryService;

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(PatientId.generate());
        patient.setMrn(new MRN("MRN123"));
        patient.setSsn(new SSN("SSN123"));
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
    }

    @Test
    void getPatientById_shouldReturnPatientResponse() {
        when(patientRepository.findById(any(PatientId.class))).thenReturn(Optional.of(patient));

        var response = patientQueryService.getPatientById(patient.getId().getId());

        assertNotNull(response);
        assertEquals("John", response.getFirstName());
    }

    @Test
    void getPatientById_shouldThrowExceptionWhenNotFound() {
        when(patientRepository.findById(any(PatientId.class))).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> patientQueryService.getPatientById(UUID.randomUUID()));
    }

    @Test
    void searchPatients_byMrn_shouldReturnPatientResponseList() {
        when(patientRepository.findByMrn(any(MRN.class))).thenReturn(Optional.of(patient));

        var response = patientQueryService.searchPatients("mrn:MRN123");

        assertFalse(response.isEmpty());
        assertEquals("John", response.get(0).getFirstName());
    }

    @Test
    void searchPatients_bySsn_shouldReturnPatientResponseList() {
        when(patientRepository.findBySsn(any(SSN.class))).thenReturn(Optional.of(patient));

        var response = patientQueryService.searchPatients("ssn:SSN123");

        assertFalse(response.isEmpty());
        assertEquals("John", response.get(0).getFirstName());
    }

    @Test
    void searchPatients_byFirstName_shouldReturnPatientResponseList() {
        when(patientRepository.findByFirstName(any(String.class))).thenReturn(List.of(patient));

        var response = patientQueryService.searchPatients("firstName:John");

        assertFalse(response.isEmpty());
        assertEquals("John", response.get(0).getFirstName());
    }

    @Test
    void searchPatients_byLastName_shouldReturnPatientResponseList() {
        when(patientRepository.findByLastName(any(String.class))).thenReturn(List.of(patient));

        var response = patientQueryService.searchPatients("lastName:Doe");

        assertFalse(response.isEmpty());
        assertEquals("John", response.get(0).getFirstName());
    }

    @Test
    void searchPatients_noMatch_shouldReturnEmptyList() {
        when(patientRepository.findByMrn(any(MRN.class))).thenReturn(Optional.empty());

        var response = patientQueryService.searchPatients("mrn:NOMATCH");

        assertTrue(response.isEmpty());
    }

    @Test
    void getPatientTimeline_shouldReturnPatientTimelineResponse() {
        when(patientRepository.findById(any(PatientId.class))).thenReturn(Optional.of(patient));

        var response = patientQueryService.getPatientTimeline(patient.getId().getId());

        assertNotNull(response);
        assertEquals(patient.getId().getId().toString(), response.getPatientId());
    }

    @Test
    void getPatientTimeline_shouldThrowExceptionWhenPatientNotFound() {
        when(patientRepository.findById(any(PatientId.class))).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> patientQueryService.getPatientTimeline(UUID.randomUUID()));
    }
}
