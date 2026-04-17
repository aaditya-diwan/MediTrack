package com.meditrack.insurance_service.interfaces.rest;

import com.meditrack.insurance_service.application.exception.DuplicatePolicyException;
import com.meditrack.insurance_service.application.exception.InsuranceServiceException;
import com.meditrack.insurance_service.application.exception.PolicyNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicatePolicyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePolicy(DuplicatePolicyException ex, WebRequest request) {
        log.warn("Duplicate policy [path={}]: {}", request.getDescription(false), ex.getMessage());
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePolicyNotFound(PolicyNotFoundException ex, WebRequest request) {
        log.warn("Policy not found: {} [path={}]", ex.getMessage(), request.getDescription(false));
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(InsuranceServiceException.class)
    public ResponseEntity<ErrorResponse> handleInsuranceServiceException(InsuranceServiceException ex, WebRequest request) {
        log.error("Insurance service error [path={}]", request.getDescription(false), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Insurance Service Error", ex.getMessage(), request);
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
