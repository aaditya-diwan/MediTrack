package com.meditrack.patient.application.service;

import com.meditrack.patient.domain.model.ContactInfo;
import com.meditrack.patient.domain.model.Insurance;
import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.MRN;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.domain.repository.PatientRepository;
import com.meditrack.patient.infrastructure.messaging.event.PatientCreatedEvent;
import com.meditrack.patient.interfaces.dto.request.CreatePatientRequest;
import com.meditrack.patient.interfaces.dto.request.UpdatePatientRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PatientCommandServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // Constructed manually so we can provide a real MeterRegistry (SimpleMeterRegistry is in-memory, no infra needed)
    private PatientCommandService patientCommandService;

    private Patient patient;
    private CreatePatientRequest createPatientRequest;
    private UpdatePatientRequest updatePatientRequest;

    @BeforeEach
    void setUp() {
        patientCommandService = new PatientCommandService(patientRepository, new SimpleMeterRegistry(), eventPublisher);

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
    void createPatient_shouldPublishPatientCreatedEventWithoutSsn() {
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        patientCommandService.createPatient(createPatientRequest);

        ArgumentCaptor<PatientCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PatientCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PatientCreatedEvent event = eventCaptor.getValue();
        assertNotNull(event.getEventId());
        assertEquals("patient.created.v1", event.getEventType());
        assertEquals("patient-service", event.getSource());
        assertNotNull(event.getPatient());
        assertEquals(patient.getId().getId().toString(), event.getPatient().getPatientId());
        assertEquals("MRN123", event.getPatient().getMrn());
        assertEquals("John", event.getPatient().getFirstName());
        assertEquals("Doe", event.getPatient().getLastName());
        assertEquals("1990-01-01", event.getPatient().getDateOfBirth());
        assertNotNull(event.getPatient().getCreatedAt());

        // The event must never carry the SSN
        assertFalse(event.toString().contains("SSN123"));
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
