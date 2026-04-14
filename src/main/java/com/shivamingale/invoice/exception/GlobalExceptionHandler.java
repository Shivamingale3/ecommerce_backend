package com.shivamingale.invoice.exception;

import com.shivamingale.invoice.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .rejectedValue(fe.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request body failed validation")
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();

        log.warn("Validation failed at {}: {} field errors", request.getRequestURI(), fieldErrors.size());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .rejectedValue(fe.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Binding Failed")
                .message("Request binding failed")
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .timestamp(Instant.now())
                .build();

        log.warn("Resource not found: {} at {}", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleViolationException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Business Rule Violation")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .timestamp(Instant.now())
                .build();

        log.warn("Business rule violation: {} at {}", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(422).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .timestamp(Instant.now())
                .build();

        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        String traceId = getTraceId();

        log.error("Unhandled exception at {} [traceId={}]", request.getRequestURI(), traceId, ex);

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .traceId(traceId)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null) ? traceId : "N/A";
    }
}
