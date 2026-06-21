package com.lowkeyarhan.TypeAhead.modules.common.exception;

import org.springframework.http.HttpStatus;

// Base exception class for application-specific API errors.
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ApiException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
