package com.meditrack.patient.interfaces.rest;

import com.meditrack.patient.application.exception.DuplicatePatientException;
import com.meditrack.patient.application.exception.MedicalRecordNotFoundException;
import com.meditrack.patient.application.exception.PatientNotFoundException;
import com.meditrack.patient.application.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {} [path={}]", ex.getMessage(), request.getDescription(false));
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePatientNotFoundException(PatientNotFoundException ex, WebRequest request) {
        log.warn("Patient not found: {} [path={}]", ex.getMessage(), request.getDescription(false));
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(MedicalRecordNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMedicalRecordNotFoundException(MedicalRecordNotFoundException ex, WebRequest request) {
        log.warn("Medical record not found: {} [path={}]", ex.getMessage(), request.getDescription(false));
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicatePatientException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePatientException(DuplicatePatientException ex, WebRequest request) {
        log.warn("Duplicate patient: {} [path={}]", ex.getMessage(), request.getDescription(false));
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        // Do not log the exception itself — invalid credentials are not an application error
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid username or password", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed [path={}, violations={}]", request.getDescription(false), violations);
        return build(HttpStatus.BAD_REQUEST, "Bad Request", violations, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Unhandled exception [path={}]", request.getDescription(false), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, WebRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), error, message, request.getDescription(false)));
    }
}
