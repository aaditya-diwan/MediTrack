package com.meditrack.patient.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.patient.application.exception.PatientNotFoundException;
import com.meditrack.patient.application.usecase.CreatePatientUseCase;
import com.meditrack.patient.application.usecase.GetPatientUseCase;
import com.meditrack.patient.application.usecase.UpdatePatientUseCase;
import com.meditrack.patient.interfaces.dto.request.CreatePatientRequest;
import com.meditrack.patient.interfaces.dto.request.UpdatePatientRequest;
import com.meditrack.patient.interfaces.dto.response.PatientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = PatientController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreatePatientUseCase createPatientUseCase;

    @MockBean
    private UpdatePatientUseCase updatePatientUseCase;

    @MockBean
    private GetPatientUseCase getPatientUseCase;

    private PatientResponse patientResponse;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        patientResponse = new PatientResponse();
        patientResponse.setId(patientId.toString());
        patientResponse.setFirstName("John");
        patientResponse.setLastName("Doe");
        patientResponse.setMrn("MRN-001");
        patientResponse.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patientResponse.setEmail("john.doe@example.com");
        patientResponse.setPhoneNumber("555-1234");
        patientResponse.setAddress("123 Main St");
        patientResponse.setInsuranceProvider("Blue Cross");
        patientResponse.setInsurancePolicyNumber("POL-001");
    }

    @Test
    void createPatient_validRequest_returns201() throws Exception {
        CreatePatientRequest request = new CreatePatientRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setMrn("MRN-001");
        request.setSsn("123-45-6789");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setEmail("john.doe@example.com");
        request.setPhoneNumber("555-1234");
        request.setAddress("123 Main St");

        when(createPatientUseCase.createPatient(any(CreatePatientRequest.class))).thenReturn(patientResponse);

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.mrn").value("MRN-001"));
    }

    @Test
    void getPatientById_existingId_returns200() throws Exception {
        when(getPatientUseCase.getPatientById(eq(patientId))).thenReturn(patientResponse);

        mockMvc.perform(get("/api/v1/patients/{id}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void getPatientById_notFound_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(getPatientUseCase.getPatientById(eq(unknownId)))
                .thenThrow(new PatientNotFoundException("Patient not found"));

        mockMvc.perform(get("/api/v1/patients/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updatePatient_existingId_returns200() throws Exception {
        UpdatePatientRequest request = new UpdatePatientRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setMrn("MRN-001");
        request.setSsn("123-45-6789");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        when(updatePatientUseCase.updatePatient(eq(patientId), any(UpdatePatientRequest.class)))
                .thenReturn(patientResponse);

        mockMvc.perform(put("/api/v1/patients/{id}", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John")); // mock returns original response
    }
}
