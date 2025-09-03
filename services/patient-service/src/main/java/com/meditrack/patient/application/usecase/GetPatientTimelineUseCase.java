package com.meditrack.patient.application.usecase;

import com.meditrack.patient.interfaces.dto.response.PatientTimelineResponse;

import java.util.UUID;

public interface GetPatientTimelineUseCase {
    PatientTimelineResponse getPatientTimeline(UUID patientId);
}
