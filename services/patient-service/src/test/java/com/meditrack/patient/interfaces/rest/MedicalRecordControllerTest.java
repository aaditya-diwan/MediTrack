package com.meditrack.patient.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.patient.application.exception.MedicalRecordNotFoundException;
import com.meditrack.patient.application.service.MedicalRecordApplicationService;
import com.meditrack.patient.interfaces.dto.request.CreateMedicalRecordRequest;
import com.meditrack.patient.interfaces.dto.request.UpdateMedicalRecordRequest;
import com.meditrack.patient.interfaces.dto.response.MedicalRecordResponse;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = MedicalRecordController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class MedicalRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MedicalRecordApplicationService medicalRecordApplicationService;

    private MedicalRecordResponse medicalRecordResponse;
    private String recordId;

    @BeforeEach
    void setUp() {
        recordId = UUID.randomUUID().toString();
        medicalRecordResponse = new MedicalRecordResponse();
        medicalRecordResponse.setRecordId(recordId);
        medicalRecordResponse.setDiagnosis("Influenza");
        medicalRecordResponse.setTreatment("Rest and fluids");
        medicalRecordResponse.setDate(LocalDate.of(2024, 3, 15));
    }

    @Test
    void createMedicalRecord_validRequest_returns201() throws Exception {
        CreateMedicalRecordRequest request = new CreateMedicalRecordRequest();
        request.setPatientId(UUID.randomUUID());
        request.setDiagnosis("Influenza");
        request.setTreatment("Rest and fluids");
        request.setDate(LocalDate.of(2024, 3, 15));

        when(medicalRecordApplicationService.createMedicalRecord(any(CreateMedicalRecordRequest.class)))
                .thenReturn(medicalRecordResponse);

        mockMvc.perform(post("/api/v1/medical-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diagnosis").value("Influenza"))
                .andExpect(jsonPath("$.treatment").value("Rest and fluids"));
    }

    @Test
    void getMedicalRecordById_existing_returns200() throws Exception {
        when(medicalRecordApplicationService.getMedicalRecordById(eq(recordId)))
                .thenReturn(medicalRecordResponse);

        mockMvc.perform(get("/api/v1/medical-records/{recordId}", recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId").value(recordId))
                .andExpect(jsonPath("$.diagnosis").value("Influenza"));
    }

    @Test
    void getMedicalRecordById_notFound_returns404() throws Exception {
        when(medicalRecordApplicationService.getMedicalRecordById(any(String.class)))
                .thenThrow(new MedicalRecordNotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/medical-records/{recordId}", "nonexistent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getMedicalRecordsByPatientId_returns200WithList() throws Exception {
        UUID patientId = UUID.randomUUID();
        when(medicalRecordApplicationService.getMedicalRecordsByPatientId(eq(patientId)))
                .thenReturn(List.of(medicalRecordResponse));

        mockMvc.perform(get("/api/v1/medical-records/patient/{patientId}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosis").value("Influenza"));
    }

    @Test
    void updateMedicalRecord_existing_returns200() throws Exception {
        UpdateMedicalRecordRequest request = new UpdateMedicalRecordRequest();
        request.setDiagnosis("Pneumonia");
        request.setTreatment("Antibiotics");
        request.setDate(LocalDate.of(2024, 3, 20));

        when(medicalRecordApplicationService.updateMedicalRecord(eq(recordId), any(UpdateMedicalRecordRequest.class)))
                .thenReturn(medicalRecordResponse);

        mockMvc.perform(put("/api/v1/medical-records/{recordId}", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId").value(recordId));
    }

    @Test
    void deleteMedicalRecord_existing_returns204() throws Exception {
        doNothing().when(medicalRecordApplicationService).deleteMedicalRecord(eq(recordId));

        mockMvc.perform(delete("/api/v1/medical-records/{recordId}", recordId))
                .andExpect(status().isNoContent());
    }
}
