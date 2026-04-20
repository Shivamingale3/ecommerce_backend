package com.shivamingale.ecom.exception;

import org.springframework.http.HttpStatus;

public class ConcurrencyException extends HttpException {

    public ConcurrencyException(String message) {
        super(HttpStatus.CONFLICT, message, "CONCURRENCY_ERROR");
    }

    public ConcurrencyException(String message, String errorCode) {
        super(HttpStatus.CONFLICT, message, errorCode);
    }

    public ConcurrencyException(String message, String errorCode, String trace) {
        super(HttpStatus.CONFLICT, message, errorCode, trace);
    }

    public ConcurrencyException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.CONFLICT, message, errorCode, trace, cause);
    }
}
