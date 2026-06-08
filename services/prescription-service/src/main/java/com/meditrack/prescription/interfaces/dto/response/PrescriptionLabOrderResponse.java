package com.meditrack.prescription.interfaces.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data @Builder
public class PrescriptionLabOrderResponse {
    private UUID id;
    private String testCode;
    private String testName;
    private String clinicalIndication;
    private String urgency;
}
