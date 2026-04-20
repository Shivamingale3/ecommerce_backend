package com.shivamingale.ecom.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends HttpException {

    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public ServiceUnavailableException(String message, String errorCode) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, errorCode);
    }

    public ServiceUnavailableException(String message, String errorCode, String trace) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, errorCode, trace);
    }

    public ServiceUnavailableException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, errorCode, trace, cause);
    }
}
