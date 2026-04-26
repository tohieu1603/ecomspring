package com.hieu.shipping_service.exception;

import com.hieu.common.error.ErrorResponse;
import com.hieu.common.error.ErrorResponse.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Maps domain exceptions to stable JSON error responses. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ShipmentNotFoundException ex, HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, "SHIP-404", ex.getMessage(), req, null);
    }

    @ExceptionHandler(DuplicateShipmentException.class)
    public ResponseEntity<ErrorResponse> conflict(DuplicateShipmentException ex, HttpServletRequest req) {
        return body(HttpStatus.CONFLICT, "SHIP-409", ex.getMessage(), req, null);
    }

    @ExceptionHandler(InvalidShipmentStateException.class)
    public ResponseEntity<ErrorResponse> invalidState(InvalidShipmentStateException ex, HttpServletRequest req) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, "SHIP-422", ex.getMessage(), req, null);
    }

    @ExceptionHandler(ShipmentAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(ShipmentAccessDeniedException ex, HttpServletRequest req) {
        return body(HttpStatus.FORBIDDEN, "SHIP-403", ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> beanValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .toList();
        return body(HttpStatus.BAD_REQUEST, "APP-400", "Validation failed", req, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "APP-400", ex.getMessage(), req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> dataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT, "APP-409", "Constraint violation", req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> springAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return body(HttpStatus.FORBIDDEN, "APP-403", "Access denied", req, null);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> unauthenticated(AuthenticationCredentialsNotFoundException ex, HttpServletRequest req) {
        return body(HttpStatus.UNAUTHORIZED, "APP-401", "Authentication required", req, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, "APP-400", ex.getMessage(), req, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> illegalState(IllegalStateException ex, HttpServletRequest req) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, "APP-422", ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unknown(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "APP-500", "Internal server error", req, null);
    }

    private static ResponseEntity<ErrorResponse> body(HttpStatus status, String code, String message,
                                                        HttpServletRequest req, List<FieldError> fields) {
        return ResponseEntity.status(status).body(
                new ErrorResponse(code, message, req.getRequestURI(), Instant.now(), fields, null, Map.of()));
    }
}
