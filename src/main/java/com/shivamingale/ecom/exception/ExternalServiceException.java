package com.shivamingale.ecom.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends HttpException {

    public ExternalServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, message, "EXTERNAL_SERVICE_ERROR");
    }

    public ExternalServiceException(String message, String errorCode) {
        super(HttpStatus.BAD_GATEWAY, message, errorCode);
    }

    public ExternalServiceException(String message, String errorCode, String trace) {
        super(HttpStatus.BAD_GATEWAY, message, errorCode, trace);
    }

    public ExternalServiceException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, errorCode, trace, cause);
    }
}
