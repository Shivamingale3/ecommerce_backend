package com.shivamingale.invoice.exception;

import org.springframework.http.HttpStatus;

public class HttpException extends AppException {

    public HttpException(HttpStatus status, String message) {
        super(status, message);
    }

    public HttpException(HttpStatus status, String message, String errorCode) {
        super(status, message, errorCode);
    }

    public HttpException(HttpStatus status, String message, String errorCode, String trace) {
        super(status, message, errorCode, trace);
    }

    public HttpException(HttpStatus status, String message, String errorCode, String trace, Throwable cause) {
        super(status, message, errorCode, trace, cause);
    }
}
