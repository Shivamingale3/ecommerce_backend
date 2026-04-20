package com.shivamingale.invoice.exception;

import org.springframework.http.HttpStatus;

public class PayloadTooLargeException extends HttpException {

    public PayloadTooLargeException(String message) {
        super(HttpStatus.valueOf(413), message);
    }

    public PayloadTooLargeException(String message, String errorCode) {
        super(HttpStatus.valueOf(413), message, errorCode);
    }

    public PayloadTooLargeException(String message, String errorCode, String trace) {
        super(HttpStatus.valueOf(413), message, errorCode, trace);
    }

    public PayloadTooLargeException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.valueOf(413), message, errorCode, trace, cause);
    }
}
