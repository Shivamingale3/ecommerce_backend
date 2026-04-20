package com.shivamingale.ecom.exception;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.shivamingale.ecom.config.TraceProperties;
import com.shivamingale.ecom.dto.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final TraceProperties traceProperties;

    public GlobalExceptionHandler(TraceProperties traceProperties) {
        this.traceProperties = traceProperties;
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(
            AppException ex, HttpServletRequest request) {

        boolean includeTrace = traceProperties.isEnabled();
        String traceId = getTraceId();
        String traceValue = includeTrace ? (ex.getTrace() != null ? ex.getTrace() : traceId) : null;

        ErrorResponse body = ErrorResponse.builder()
                .status(ex.getStatus())
                .error(getErrorName(ex.getStatus()))
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(traceId)
                .timestamp(Instant.now())
                .fieldErrors(null)
                .build();

        if (includeTrace && traceValue != null) {
            body.setTrace(traceValue);
        }

        log.warn("App exception [{}] at {}: {}", ex.getStatus(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(
                        fe -> ErrorResponse.FieldError.builder()
                                .field(fe.getField())
                                .message(fe.getDefaultMessage())
                                .rejectedValue(fe.getRejectedValue())
                                .build())
                .collect(Collectors.toList());

        String traceId = getTraceId();
        boolean includeTrace = traceProperties.isEnabled();

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .error("Validation Failed")
                .message("Request body failed validation")
                .path(request.getRequestURI())
                .traceId(traceId)
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();

        if (includeTrace) {
            body.setTrace(ex.getMessage());
        }

        log.warn(
                "Validation failed at {}: {} field errors",
                request.getRequestURI(),
                fieldErrors.size());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getFieldErrors().stream()
                .map(
                        fe -> ErrorResponse.FieldError.builder()
                                .field(fe.getField())
                                .message(fe.getDefaultMessage())
                                .rejectedValue(fe.getRejectedValue())
                                .build())
                .collect(Collectors.toList());

        String traceId = getTraceId();
        boolean includeTrace = traceProperties.isEnabled();

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .error("Binding Failed")
                .message("Request binding failed")
                .path(request.getRequestURI())
                .traceId(traceId)
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();

        if (includeTrace) {
            body.setTrace(ex.getMessage());
        }

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        String traceId = getTraceId();
        boolean includeTrace = traceProperties.isEnabled();

        log.error("Unhandled exception at {} [traceId={}]", request.getRequestURI(), traceId, ex);

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .traceId(traceId)
                .timestamp(Instant.now())
                .build();

        if (includeTrace) {
            body.setTrace(ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null) ? traceId : "N/A";
    }

    private String getErrorName(HttpStatus status) {
        try {
            return status.getReasonPhrase();
        } catch (Exception e) {
            return "Error";
        }
    }
}
