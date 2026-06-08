package com.meditrack.doctor.application.exception;

import java.util.UUID;

public class DoctorNotFoundException extends RuntimeException {
    public DoctorNotFoundException(UUID id) {
        super("Doctor not found with id: " + id);
    }
    public DoctorNotFoundException(String message) {
        super(message);
    }
}
