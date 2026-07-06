package com.meditrack.appointment.domain.port;

/**
 * Signals that the doctor directory (doctor-service) could not be reached or
 * answered with an unexpected error. The booking flow treats this as a
 * fail-open condition: it logs and proceeds rather than blocking all bookings
 * during an outage.
 */
public class DoctorDirectoryUnavailableException extends RuntimeException {
    public DoctorDirectoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
