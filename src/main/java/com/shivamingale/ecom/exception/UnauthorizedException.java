package com.shivamingale.ecom.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends HttpException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    public UnauthorizedException(String message, String errorCode) {
        super(HttpStatus.UNAUTHORIZED, message, errorCode);
    }

    public UnauthorizedException(String message, String errorCode, String trace) {
        super(HttpStatus.UNAUTHORIZED, message, errorCode, trace);
    }

    public UnauthorizedException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED, message, errorCode, trace, cause);
    }
}
