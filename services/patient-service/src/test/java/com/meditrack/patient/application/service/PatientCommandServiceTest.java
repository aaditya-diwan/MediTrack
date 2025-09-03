package com.meditrack.patient.application.service;

import com.meditrack.patient.domain.model.ContactInfo;
import com.meditrack.patient.domain.model.Insurance;
import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.MRN;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.domain.repository.PatientRepository;
import com.meditrack.patient.interfaces.dto.request.CreatePatientRequest;
import com.meditrack.patient.interfaces.dto.request.UpdatePatientRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PatientCommandServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientCommandService patientCommandService;

    private Patient patient;
    private CreatePatientRequest createPatientRequest;
    private UpdatePatientRequest updatePatientRequest;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(PatientId.generate());
        patient.setMrn(new MRN("MRN123"));
        patient.setSsn(new SSN("SSN123"));
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setContactInfo(new ContactInfo());
        patient.setInsurance(new Insurance());

        createPatientRequest = new CreatePatientRequest();
        createPatientRequest.setMrn("MRN123");
        createPatientRequest.setSsn("SSN123");
        createPatientRequest.setFirstName("John");
        createPatientRequest.setLastName("Doe");
        createPatientRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));
        createPatientRequest.setEmail("john.doe@example.com");
        createPatientRequest.setPhoneNumber("123-456-7890");
        createPatientRequest.setAddress("123 Main St");
        createPatientRequest.setInsuranceProvider("ProviderA");
        createPatientRequest.setInsurancePolicyNumber("Policy123");

        updatePatientRequest = new UpdatePatientRequest();
        updatePatientRequest.setMrn("MRN456");
        updatePatientRequest.setSsn("SSN456");
        updatePatientRequest.setFirstName("Jane");
        updatePatientRequest.setLastName("Smith");
        updatePatientRequest.setDateOfBirth(LocalDate.of(1995, 5, 5));
        updatePatientRequest.setEmail("jane.smith@example.com");
        updatePatientRequest.setPhoneNumber("098-765-4321");
        updatePatientRequest.setAddress("456 Oak Ave");
        updatePatientRequest.setInsuranceProvider("ProviderB");
        updatePatientRequest.setInsurancePolicyNumber("Policy456");
    }

    @Test
    void createPatient_shouldReturnPatientResponse() {
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        var response = patientCommandService.createPatient(createPatientRequest);

        assertNotNull(response);
        assertEquals(patient.getFirstName(), response.getFirstName());
    }

    @Test
    void updatePatient_shouldReturnUpdatedPatientResponse() {
        UUID patientId = patient.getId().getId();
        when(patientRepository.findById(any(PatientId.class))).thenReturn(Optional.of(patient));
        patient.getContactInfo().setEmail(updatePatientRequest.getEmail());
        patient.getContactInfo().setPhoneNumber(updatePatientRequest.getPhoneNumber());
        patient.getContactInfo().setAddress(updatePatientRequest.getAddress());
        patient.getInsurance().setProvider(updatePatientRequest.getInsuranceProvider());
        patient.getInsurance().setPolicyNumber(updatePatientRequest.getInsurancePolicyNumber());

        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        var response = patientCommandService.updatePatient(patientId, updatePatientRequest);

        assertNotNull(response);
        assertEquals(updatePatientRequest.getFirstName(), response.getFirstName());
    }
}
