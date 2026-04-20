package com.shivamingale.ecom.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends HttpException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public BadRequestException(String message, String errorCode) {
        super(HttpStatus.BAD_REQUEST, message, errorCode);
    }

    public BadRequestException(String message, String errorCode, String trace) {
        super(HttpStatus.BAD_REQUEST, message, errorCode, trace);
    }

    public BadRequestException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message, errorCode, trace, cause);
    }
}
