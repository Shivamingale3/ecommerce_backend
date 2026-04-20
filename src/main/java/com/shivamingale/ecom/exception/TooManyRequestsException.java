package com.shivamingale.ecom.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends HttpException {

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    public TooManyRequestsException(String message, String errorCode) {
        super(HttpStatus.TOO_MANY_REQUESTS, message, errorCode);
    }

    public TooManyRequestsException(String message, String errorCode, String trace) {
        super(HttpStatus.TOO_MANY_REQUESTS, message, errorCode, trace);
    }

    public TooManyRequestsException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.TOO_MANY_REQUESTS, message, errorCode, trace, cause);
    }
}
