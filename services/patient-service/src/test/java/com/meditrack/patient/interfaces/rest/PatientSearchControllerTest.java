//package com.meditrack.patient.interfaces.rest;
//
//import com.meditrack.patient.application.usecase.GetPatientTimelineUseCase;
//import com.meditrack.patient.application.usecase.SearchPatientsUseCase;
//import com.meditrack.patient.interfaces.dto.response.PatientResponse;
//import com.meditrack.patient.interfaces.dto.response.PatientTimelineResponse;
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
//import java.util.Collections;
//import java.util.List;
//import java.util.UUID;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//public class PatientSearchControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private SearchPatientsUseCase searchPatientsUseCase;
//
//    @MockBean
//    private GetPatientTimelineUseCase getPatientTimelineUseCase;
//
//    private PatientResponse patientResponse;
//    private PatientTimelineResponse patientTimelineResponse;
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
//
//        patientTimelineResponse = new PatientTimelineResponse();
//        patientTimelineResponse.setPatientId(patientResponse.getId());
//        patientTimelineResponse.setTimeline(Collections.emptyList());
//    }
//
//    @Test
//    void searchPatients_shouldReturnListOfPatients() throws Exception {
//        when(searchPatientsUseCase.searchPatients(any(String.class))).thenReturn(List.of(patientResponse));
//
//        mockMvc.perform(get("/api/v1/patients/search")
//                        .param("query", "firstName:John")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].firstName").value("John"));
//    }
//
//    @Test
//    void getPatientTimeline_shouldReturnPatientTimeline() throws Exception {
//        when(getPatientTimelineUseCase.getPatientTimeline(any(UUID.class))).thenReturn(patientTimelineResponse);
//
//        mockMvc.perform(get("/api/v1/patients/search/{id}/timeline", UUID.randomUUID())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath(".patientId").value(patientTimelineResponse.getPatientId()));
//    }
//}
