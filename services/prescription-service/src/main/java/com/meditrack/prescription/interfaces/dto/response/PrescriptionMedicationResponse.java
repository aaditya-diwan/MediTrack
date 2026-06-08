package com.meditrack.prescription.interfaces.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data @Builder
public class PrescriptionMedicationResponse {
    private UUID id;
    private String medicationName;
    private String genericName;
    private String dosage;
    private String frequency;
    private String duration;
    private String route;
    private String instructions;
}
