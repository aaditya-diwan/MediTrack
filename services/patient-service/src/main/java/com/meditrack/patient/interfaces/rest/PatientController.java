package com.meditrack.patient.interfaces.rest;

import com.meditrack.patient.application.usecase.CreatePatientUseCase;
import com.meditrack.patient.application.usecase.GetPatientUseCase;
import com.meditrack.patient.application.usecase.UpdatePatientUseCase;
import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.interfaces.dto.request.CreatePatientRequest;
import com.meditrack.patient.interfaces.dto.request.UpdatePatientRequest;
import com.meditrack.patient.interfaces.dto.response.PatientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final CreatePatientUseCase createPatientUseCase;
    private final UpdatePatientUseCase updatePatientUseCase;
    private final GetPatientUseCase getPatientUseCase;

    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(@RequestBody CreatePatientRequest request) {
        PatientResponse response = createPatientUseCase.createPatient(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponse> updatePatient(@PathVariable UUID id, @RequestBody UpdatePatientRequest request) {
        PatientResponse response = updatePatientUseCase.updatePatient(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> getPatientById(@PathVariable UUID id) {
        PatientResponse response = getPatientUseCase.getPatientById(id);
        return ResponseEntity.ok(response);
    }

    
}
