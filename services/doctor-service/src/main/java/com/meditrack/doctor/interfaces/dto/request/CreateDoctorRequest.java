package com.meditrack.doctor.interfaces.dto.request;

import com.meditrack.doctor.domain.model.Specialization;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDoctorRequest {
    @NotBlank
    private String employeeId;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank @Email
    private String email;
    private String phone;
    @NotNull
    private Specialization specialization;
    private String qualifications;
    private Integer yearsOfExperience;
    private String bio;
}
