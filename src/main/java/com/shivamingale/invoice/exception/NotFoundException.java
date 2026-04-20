package com.shivamingale.invoice.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends HttpException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public NotFoundException(String message, String errorCode) {
        super(HttpStatus.NOT_FOUND, message, errorCode);
    }

    public NotFoundException(String message, String errorCode, String trace) {
        super(HttpStatus.NOT_FOUND, message, errorCode, trace);
    }

    public NotFoundException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.NOT_FOUND, message, errorCode, trace, cause);
    }
}
