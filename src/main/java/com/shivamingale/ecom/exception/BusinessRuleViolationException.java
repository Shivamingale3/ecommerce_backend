package com.shivamingale.ecom.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleViolationException extends HttpException {

    public BusinessRuleViolationException(String message) {
        super(HttpStatus.valueOf(422), message, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessRuleViolationException(String message, String errorCode) {
        super(HttpStatus.valueOf(422), message, errorCode);
    }

    public BusinessRuleViolationException(String message, String errorCode, String trace) {
        super(HttpStatus.valueOf(422), message, errorCode, trace);
    }

    public BusinessRuleViolationException(String message, String errorCode, String trace, Throwable cause) {
        super(HttpStatus.valueOf(422), message, errorCode, trace, cause);
    }
}
