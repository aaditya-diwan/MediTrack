package com.meditrack.patient.interfaces.rest;

import com.meditrack.patient.application.service.MedicalRecordApplicationService;
import com.meditrack.patient.interfaces.dto.request.CreateMedicalRecordRequest;
import com.meditrack.patient.interfaces.dto.request.UpdateMedicalRecordRequest;
import com.meditrack.patient.interfaces.dto.response.MedicalRecordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordApplicationService medicalRecordApplicationService;

    @PostMapping
    public ResponseEntity<MedicalRecordResponse> createMedicalRecord(@RequestBody CreateMedicalRecordRequest request) {
        MedicalRecordResponse response = medicalRecordApplicationService.createMedicalRecord(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<MedicalRecordResponse> updateMedicalRecord(@PathVariable String recordId, @RequestBody UpdateMedicalRecordRequest request) {
        MedicalRecordResponse response = medicalRecordApplicationService.updateMedicalRecord(recordId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<MedicalRecordResponse> getMedicalRecordById(@PathVariable String recordId) {
        MedicalRecordResponse response = medicalRecordApplicationService.getMedicalRecordById(recordId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecordResponse>> getMedicalRecordsByPatientId(@PathVariable UUID patientId) {
        List<MedicalRecordResponse> responses = medicalRecordApplicationService.getMedicalRecordsByPatientId(patientId);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteMedicalRecord(@PathVariable String recordId) {
        medicalRecordApplicationService.deleteMedicalRecord(recordId);
        return ResponseEntity.noContent().build();
    }
}
