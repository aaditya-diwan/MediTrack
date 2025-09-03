package com.meditrack.patient.application.usecase;

import com.meditrack.patient.interfaces.dto.response.PatientResponse;

import java.util.List;

public interface SearchPatientsUseCase {
    List<PatientResponse> searchPatients(String query);
}
