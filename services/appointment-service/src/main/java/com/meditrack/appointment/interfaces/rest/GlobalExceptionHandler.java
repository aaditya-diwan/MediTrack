package com.meditrack.appointment.interfaces.rest;

import com.meditrack.appointment.application.exception.AppointmentNotFoundException;
import com.meditrack.appointment.application.exception.DoctorInactiveException;
import com.meditrack.appointment.application.exception.DoctorNotFoundException;
import com.meditrack.appointment.application.exception.OutsideAvailabilityException;
import com.meditrack.appointment.application.exception.SlotAlreadyBookedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(AppointmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(SlotAlreadyBookedException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(SlotAlreadyBookedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler({DoctorNotFoundException.class, DoctorInactiveException.class})
    public ResponseEntity<Map<String, Object>> handleUnprocessableDoctor(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(OutsideAvailabilityException.class)
    public ResponseEntity<Map<String, Object>> handleOutsideAvailability(OutsideAvailabilityException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "timestamp", Instant.now().toString()));
    }
}
