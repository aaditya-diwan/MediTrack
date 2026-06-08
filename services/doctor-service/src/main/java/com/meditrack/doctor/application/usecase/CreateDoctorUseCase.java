package com.meditrack.doctor.application.usecase;

import com.meditrack.doctor.interfaces.dto.request.CreateDoctorRequest;
import com.meditrack.doctor.interfaces.dto.response.DoctorResponse;

public interface CreateDoctorUseCase {
    DoctorResponse createDoctor(CreateDoctorRequest request);
}
