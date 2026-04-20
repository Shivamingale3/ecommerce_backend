package com.shivamingale.invoice.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends HttpException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public ConflictException(String message, String errorCode) {
        super(HttpStatus.CONFLICT, message, errorCode);
    }

    public ConflictException(String message, String errorCode, String trace) {
        super(HttpStatus.CONFLICT, message, errorCode, trace);
    }

    public ConflictException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.CONFLICT, message, errorCode, trace, cause);
    }
}
