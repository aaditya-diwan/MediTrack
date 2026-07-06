package com.meditrack.prescription.interfaces.rest;

import com.meditrack.prescription.application.usecase.*;
import com.meditrack.prescription.infrastructure.pdf.PrescriptionPdfGenerator;
import com.meditrack.prescription.interfaces.dto.request.CreatePrescriptionRequest;
import com.meditrack.prescription.interfaces.dto.request.IssuePrescriptionRequest;
import com.meditrack.prescription.interfaces.dto.response.PrescriptionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final CreatePrescriptionUseCase createPrescriptionUseCase;
    private final GetPrescriptionUseCase getPrescriptionUseCase;
    private final IssuePrescriptionUseCase issuePrescriptionUseCase;
    private final SendToPharmacyUseCase sendToPharmacyUseCase;
    private final SendToLabUseCase sendToLabUseCase;
    private final PrescriptionPdfGenerator pdfGenerator;

    @PostMapping
    public ResponseEntity<PrescriptionResponse> create(@Valid @RequestBody CreatePrescriptionRequest request) {
        return new ResponseEntity<>(createPrescriptionUseCase.createPrescription(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(getPrescriptionUseCase.getPrescriptionById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PrescriptionResponse>> getPatientPrescriptions(@PathVariable UUID patientId) {
        return ResponseEntity.ok(getPrescriptionUseCase.getPatientPrescriptions(patientId));
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<PrescriptionResponse> issue(@PathVariable UUID id,
                                                      @RequestBody(required = false) IssuePrescriptionRequest request) {
        boolean override = request != null && Boolean.TRUE.equals(request.getOverride());
        String overrideReason = request != null ? request.getOverrideReason() : null;
        return ResponseEntity.ok(issuePrescriptionUseCase.issuePrescription(id, override, overrideReason));
    }

    @PostMapping("/{id}/send-pharmacy")
    public ResponseEntity<PrescriptionResponse> sendToPharmacy(@PathVariable UUID id) {
        return ResponseEntity.ok(sendToPharmacyUseCase.sendToPharmacy(id));
    }

    @PostMapping("/{id}/send-lab")
    public ResponseEntity<PrescriptionResponse> sendToLab(@PathVariable UUID id) {
        return ResponseEntity.ok(sendToLabUseCase.sendToLab(id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        PrescriptionResponse prescription = getPrescriptionUseCase.getPrescriptionById(id);
        byte[] pdf = pdfGenerator.generatePdf(
                com.meditrack.prescription.domain.model.Prescription.builder()
                        .id(prescription.getId()).patientId(prescription.getPatientId())
                        .doctorId(prescription.getDoctorId()).consultationNotes(prescription.getConsultationNotes())
                        .issuedAt(prescription.getIssuedAt()).build());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "prescription-" + id + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
