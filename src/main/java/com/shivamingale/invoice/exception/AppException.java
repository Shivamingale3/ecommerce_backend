package com.shivamingale.invoice.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String trace;

    public AppException(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    public AppException(HttpStatus status, String message, String errorCode) {
        this(status, message, errorCode, null);
    }

    public AppException(HttpStatus status, String message, String errorCode, String trace) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.trace = trace;
    }

    public AppException(HttpStatus status, String message, String errorCode, String trace, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.trace = trace;
    }
}
