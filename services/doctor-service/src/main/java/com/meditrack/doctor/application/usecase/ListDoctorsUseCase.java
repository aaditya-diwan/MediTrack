package com.meditrack.doctor.application.usecase;

import com.meditrack.doctor.domain.model.Specialization;
import com.meditrack.doctor.interfaces.dto.response.DoctorResponse;

import java.util.List;

public interface ListDoctorsUseCase {
    List<DoctorResponse> listAllDoctors();
    List<DoctorResponse> listBySpecialization(Specialization specialization);
}
