package com.meditrack.doctor.interfaces.dto.response;

import com.meditrack.doctor.domain.model.Specialization;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DoctorResponse {
    private UUID id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private Specialization specialization;
    private String qualifications;
    private Integer yearsOfExperience;
    private String bio;
    private boolean active;
}
