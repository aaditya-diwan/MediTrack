package com.meditrack.prescription.interfaces.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class PrescriptionResponse {
    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private UUID appointmentId;
    private String status;
    private String consultationNotes;
    private String diagnosisCodes;
    private List<PrescriptionMedicationResponse> medications;
    private List<PrescriptionLabOrderResponse> labOrders;
    private LocalDateTime issuedAt;
    private LocalDate validUntil;
    private LocalDateTime createdAt;
}
