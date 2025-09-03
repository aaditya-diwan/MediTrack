package com.meditrack.patient.application.usecase;

import com.meditrack.patient.interfaces.dto.request.CreatePatientRequest;
import com.meditrack.patient.interfaces.dto.response.PatientResponse;

public interface CreatePatientUseCase {
    PatientResponse createPatient(CreatePatientRequest request);
}
