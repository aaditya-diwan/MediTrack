package com.meditrack.prescription.application.usecase;

import com.meditrack.prescription.interfaces.dto.response.PrescriptionResponse;

import java.util.List;
import java.util.UUID;

public interface GetPrescriptionUseCase {
    PrescriptionResponse getPrescriptionById(UUID id);
    List<PrescriptionResponse> getPatientPrescriptions(UUID patientId);
}
