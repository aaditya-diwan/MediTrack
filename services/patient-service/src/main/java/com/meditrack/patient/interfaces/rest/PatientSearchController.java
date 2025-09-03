package com.meditrack.patient.interfaces.rest;

import com.meditrack.patient.application.usecase.GetPatientTimelineUseCase;
import com.meditrack.patient.application.usecase.SearchPatientsUseCase;
import com.meditrack.patient.interfaces.dto.response.PatientResponse;
import com.meditrack.patient.interfaces.dto.response.PatientTimelineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/search")
@RequiredArgsConstructor
public class PatientSearchController {

    private final SearchPatientsUseCase searchPatientsUseCase;
    private final GetPatientTimelineUseCase getPatientTimelineUseCase;

    @GetMapping
    public ResponseEntity<List<PatientResponse>> searchPatients(@RequestParam("query") String query) {
        List<PatientResponse> responses = searchPatientsUseCase.searchPatients(query);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<PatientTimelineResponse> getPatientTimeline(@PathVariable UUID id) {
        PatientTimelineResponse response = getPatientTimelineUseCase.getPatientTimeline(id);
        return ResponseEntity.ok(response);
    }
}
