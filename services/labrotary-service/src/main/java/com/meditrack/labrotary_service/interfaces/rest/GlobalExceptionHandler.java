package com.meditrack.labrotary_service.interfaces.rest;

import com.meditrack.labrotary_service.application.exception.LabOrderNotFoundException;
import com.meditrack.labrotary_service.application.exception.LabResultNotFoundException;
import com.meditrack.labrotary_service.application.exception.LabServiceException;
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

    @ExceptionHandler({LabOrderNotFoundException.class, LabResultNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(LabServiceException ex, WebRequest request) {
        log.warn("Resource not found: {} [path={}]", ex.getMessage(), request.getDescription(false));
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(LabServiceException.class)
    public ResponseEntity<ErrorResponse> handleLabServiceException(LabServiceException ex, WebRequest request) {
        log.error("Lab service error [path={}]", request.getDescription(false), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Lab Service Error", ex.getMessage(), request);
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
