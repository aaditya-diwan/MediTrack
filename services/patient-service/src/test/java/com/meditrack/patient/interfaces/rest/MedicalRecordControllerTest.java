//package com.meditrack.patient.interfaces.rest;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.meditrack.patient.application.service.MedicalRecordApplicationService;
//import com.meditrack.patient.interfaces.dto.request.CreateMedicalRecordRequest;
//import com.meditrack.patient.interfaces.dto.request.UpdateMedicalRecordRequest;
//import com.meditrack.patient.interfaces.dto.response.MedicalRecordResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.UUID;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//public class MedicalRecordControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private MedicalRecordApplicationService medicalRecordApplicationService;
//
//    private MedicalRecordResponse medicalRecordResponse;
//
//    @BeforeEach
//    void setUp() {
//        medicalRecordResponse = new MedicalRecordResponse();
//        medicalRecordResponse.setRecordId(UUID.randomUUID().toString());
//        medicalRecordResponse.setDiagnosis("Flu");
//        medicalRecordResponse.setTreatment("Rest and fluids");
//        medicalRecordResponse.setDate(LocalDate.now());
//    }
//
//    @Test
//    void createMedicalRecord_shouldReturnCreatedMedicalRecord() throws Exception {
//        CreateMedicalRecordRequest request = new CreateMedicalRecordRequest();
//        request.setPatientId(UUID.randomUUID());
//        request.setDiagnosis("Cold");
//
//        when(medicalRecordApplicationService.createMedicalRecord(any(CreateMedicalRecordRequest.class))).thenReturn(medicalRecordResponse);
//
//        mockMvc.perform(post("/api/v1/medical-records")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath(".diagnosis").value("Flu"));
//    }
//
//    @Test
//    void updateMedicalRecord_shouldReturnUpdatedMedicalRecord() throws Exception {
//        UpdateMedicalRecordRequest request = new UpdateMedicalRecordRequest();
//        request.setDiagnosis("Pneumonia");
//
//        when(medicalRecordApplicationService.updateMedicalRecord(any(String.class), any(UpdateMedicalRecordRequest.class))).thenReturn(medicalRecordResponse);
//
//        mockMvc.perform(put("/api/v1/medical-records/{recordId}", UUID.randomUUID().toString())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath(".diagnosis").value("Flu"));
//    }
//
//    @Test
//    void getMedicalRecordById_shouldReturnMedicalRecord() throws Exception {
//        when(medicalRecordApplicationService.getMedicalRecordById(any(String.class))).thenReturn(medicalRecordResponse);
//
//        mockMvc.perform(get("/api/v1/medical-records/{recordId}", UUID.randomUUID().toString()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath(".diagnosis").value("Flu"));
//    }
//
//    @Test
//    void getMedicalRecordsByPatientId_shouldReturnListOfMedicalRecords() throws Exception {
//        when(medicalRecordApplicationService.getMedicalRecordsByPatientId(any(UUID.class))).thenReturn(List.of(medicalRecordResponse));
//
//        mockMvc.perform(get("/api/v1/medical-records/patient/{patientId}", UUID.randomUUID()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].diagnosis").value("Flu"));
//    }
//
//    @Test
//    void deleteMedicalRecord_shouldReturnNoContent() throws Exception {
//        doNothing().when(medicalRecordApplicationService).deleteMedicalRecord(any(String.class));
//
//        mockMvc.perform(delete("/api/v1/medical-records/{recordId}", UUID.randomUUID().toString()))
//                .andExpect(status().isNoContent());
//    }
//}
