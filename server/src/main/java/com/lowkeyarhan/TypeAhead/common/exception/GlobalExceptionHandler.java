package com.lowkeyarhan.TypeAhead.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler to intercept exceptions thrown in controllers and format clean API errors.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponseDTO> handleApiException(ApiException ex, HttpServletRequest request) {
        log.error("API Exception occurred: {}", ex.getMessage(), ex);
        ErrorResponseDTO error = new ErrorResponseDTO(
            Instant.now(),
            ex.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errorMsg);
        ErrorResponseDTO error = new ErrorResponseDTO(
            Instant.now(),
            "Validation failed: " + errorMsg,
            request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        ErrorResponseDTO error = new ErrorResponseDTO(
            Instant.now(),
            "An unexpected error occurred. Please check logs for details.",
            request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
