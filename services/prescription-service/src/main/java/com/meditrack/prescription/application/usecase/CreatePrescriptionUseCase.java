package com.meditrack.prescription.application.usecase;

import com.meditrack.prescription.interfaces.dto.request.CreatePrescriptionRequest;
import com.meditrack.prescription.interfaces.dto.response.PrescriptionResponse;

public interface CreatePrescriptionUseCase {
    PrescriptionResponse createPrescription(CreatePrescriptionRequest request);
}
