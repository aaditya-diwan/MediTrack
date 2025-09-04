package com.meditrack.patient.application.service;

import com.meditrack.patient.application.usecase.*;
import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.interfaces.dto.request.CreatePatientRequest;
import com.meditrack.patient.interfaces.dto.request.UpdatePatientRequest;
import com.meditrack.patient.interfaces.dto.response.PatientResponse;
import com.meditrack.patient.interfaces.dto.response.PatientTimelineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientApplicationService implements CreatePatientUseCase, UpdatePatientUseCase, GetPatientUseCase, SearchPatientsUseCase, GetPatientTimelineUseCase {

    private final PatientCommandService patientCommandService;
    private final PatientQueryService patientQueryService;

    @Override
    public PatientResponse createPatient(CreatePatientRequest request) {
        return patientCommandService.createPatient(request);
    }

    @Override
    public PatientResponse updatePatient(UUID patientId, UpdatePatientRequest request) {
        return patientCommandService.updatePatient(patientId, request);
    }

    @Override
    public PatientResponse getPatientById(UUID id) {
        return patientQueryService.getPatientById(id);
    }

    @Override
    public List<PatientResponse> searchPatients(String query) {
        return patientQueryService.searchPatients(query);
    }

    @Override
    public PatientTimelineResponse getPatientTimeline(UUID patientId) {
        return patientQueryService.getPatientTimeline(patientId);
    }

    @Override
    public Optional<Patient> getPatientBySSN(SSN ssn) {
        return patientQueryService.getPatientBySSN(ssn);
    }
}