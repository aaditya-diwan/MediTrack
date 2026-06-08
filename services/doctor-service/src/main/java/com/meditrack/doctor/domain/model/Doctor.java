package com.meditrack.doctor.domain.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class Doctor {
    private UUID id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Specialization specialization;
    private String qualifications;
    private Integer yearsOfExperience;
    private String bio;
    private boolean active;

    public String getFullName() {
        return "Dr. " + firstName + " " + lastName;
    }
}
