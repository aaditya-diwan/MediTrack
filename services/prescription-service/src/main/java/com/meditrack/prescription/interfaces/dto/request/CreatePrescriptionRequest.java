package com.meditrack.prescription.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreatePrescriptionRequest {
    @NotNull private UUID patientId;
    @NotNull private UUID doctorId;
    private UUID appointmentId;
    private String consultationNotes;
    private String diagnosisCodes;
    private List<MedicationRequest> medications;
    private List<LabOrderRequest> labOrders;

    @Data
    public static class MedicationRequest {
        private String medicationName;
        private String genericName;
        private String dosage;
        private String frequency;
        private String duration;
        private String route;
        private String instructions;
    }

    @Data
    public static class LabOrderRequest {
        private String testCode;
        private String testName;
        private String clinicalIndication;
        private String urgency;
    }
}
