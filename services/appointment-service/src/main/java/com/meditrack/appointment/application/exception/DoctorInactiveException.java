package com.meditrack.appointment.application.exception;

import java.util.UUID;

/**
 * The referenced doctor exists but is not active and cannot take appointments.
 * Mapped to 422 Unprocessable Entity.
 */
public class DoctorInactiveException extends RuntimeException {
    public DoctorInactiveException(UUID doctorId, String fullName) {
        super("Doctor is not active and cannot be booked: "
                + (fullName != null && !fullName.isBlank() ? fullName + " (" + doctorId + ")" : doctorId));
    }
}
