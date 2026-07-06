package com.meditrack.appointment.application.exception;

/**
 * The requested time does not fall within the doctor's published availability.
 * Mapped to 409 Conflict, consistent with the double-booking case.
 */
public class OutsideAvailabilityException extends RuntimeException {
    public OutsideAvailabilityException(String message) {
        super(message);
    }
}
