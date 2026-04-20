package com.shivamingale.ecom.exception;

import org.springframework.http.HttpStatus;

public class InternalServerErrorException extends HttpException {

    public InternalServerErrorException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public InternalServerErrorException(String message, String errorCode) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, errorCode);
    }

    public InternalServerErrorException(String message, String errorCode, String trace) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, errorCode, trace);
    }

    public InternalServerErrorException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, errorCode, trace, cause);
    }
}
