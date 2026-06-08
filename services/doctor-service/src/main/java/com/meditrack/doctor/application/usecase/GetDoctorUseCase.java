package com.meditrack.doctor.application.usecase;

import com.meditrack.doctor.interfaces.dto.response.DoctorResponse;

import java.util.UUID;

public interface GetDoctorUseCase {
    DoctorResponse getDoctorById(UUID id);
}
