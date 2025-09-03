//package com.meditrack.patient.interfaces.rest;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.meditrack.patient.application.usecase.CreatePatientUseCase;
//import com.meditrack.patient.application.usecase.GetPatientUseCase;
//import com.meditrack.patient.application.usecase.UpdatePatientUseCase;
//import com.meditrack.patient.interfaces.dto.request.CreatePatientRequest;
//import com.meditrack.patient.interfaces.dto.request.UpdatePatientRequest;
//import com.meditrack.patient.interfaces.dto.response.PatientResponse;
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
//import java.util.UUID;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//public class PatientControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private CreatePatientUseCase createPatientUseCase;
//
//    @MockBean
//    private UpdatePatientUseCase updatePatientUseCase;
//
//    @MockBean
//    private GetPatientUseCase getPatientUseCase;
//
//    private PatientResponse patientResponse;
//
//    @BeforeEach
//    void setUp() {
//        patientResponse = new PatientResponse();
//        patientResponse.setId(UUID.randomUUID().toString());
//        patientResponse.setFirstName("John");
//        patientResponse.setLastName("Doe");
//        patientResponse.setMrn("MRN123");
//        patientResponse.setSsn("SSN123");
//        patientResponse.setDateOfBirth(LocalDate.of(1990, 1, 1));
//        patientResponse.setEmail("john.doe@example.com");
//        patientResponse.setPhoneNumber("123-456-7890");
//        patientResponse.setAddress("123 Main St");
//        patientResponse.setInsuranceProvider("ProviderA");
//        patientResponse.setInsurancePolicyNumber("Policy123");
//    }
//
//    @Test
//    void createPatient_shouldReturnCreatedPatient() throws Exception {
//        CreatePatientRequest request = new CreatePatientRequest();
//        request.setFirstName("John");
//        request.setLastName("Doe");
//
//        when(createPatientUseCase.createPatient(any(CreatePatientRequest.class))).thenReturn(patientResponse);
//
//        mockMvc.perform(post("/api/v1/patients")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath(".firstName").value("John"));
//    }
//
//    @Test
//    void updatePatient_shouldReturnUpdatedPatient() throws Exception {
//        UpdatePatientRequest request = new UpdatePatientRequest();
//        request.setFirstName("Jane");
//
//        when(updatePatientUseCase.updatePatient(any(UUID.class), any(UpdatePatientRequest.class))).thenReturn(patientResponse);
//
//        mockMvc.perform(put("/api/v1/patients/{id}", UUID.randomUUID())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath(".firstName").value("John")); // Still John because mock returns original patientResponse
//    }
//
//    @Test
//    void getPatientById_shouldReturnPatient() throws Exception {
//        when(getPatientUseCase.getPatientById(any(UUID.class))).thenReturn(patientResponse);
//
//        mockMvc.perform(get("/api/v1/patients/{id}", UUID.randomUUID()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath(".firstName").value("John"));
//    }
//}