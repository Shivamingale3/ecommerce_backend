package com.shivamingale.invoice.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends HttpException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    public ForbiddenException(String message, String errorCode) {
        super(HttpStatus.FORBIDDEN, message, errorCode);
    }

    public ForbiddenException(String message, String errorCode, String trace) {
        super(HttpStatus.FORBIDDEN, message, errorCode, trace);
    }

    public ForbiddenException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.FORBIDDEN, message, errorCode, trace, cause);
    }
}
