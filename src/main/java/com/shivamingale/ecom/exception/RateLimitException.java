package com.shivamingale.ecom.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends HttpException {

    public RateLimitException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message, "RATE_LIMIT_EXCEEDED");
    }

    public RateLimitException(String message, String errorCode) {
        super(HttpStatus.TOO_MANY_REQUESTS, message, errorCode);
    }

    public RateLimitException(String message, String errorCode, String trace) {
        super(HttpStatus.TOO_MANY_REQUESTS, message, errorCode, trace);
    }

    public RateLimitException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.TOO_MANY_REQUESTS, message, errorCode, trace, cause);
    }
}
