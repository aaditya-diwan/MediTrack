package com.meditrack.appointment.application.exception;

import java.util.UUID;

/**
 * The referenced doctor does not exist in the doctor directory.
 * Mapped to 422 Unprocessable Entity: the request is well-formed but
 * references an entity that cannot be booked.
 */
public class DoctorNotFoundException extends RuntimeException {
    public DoctorNotFoundException(UUID doctorId) {
        super("Doctor not found: " + doctorId);
    }
}
