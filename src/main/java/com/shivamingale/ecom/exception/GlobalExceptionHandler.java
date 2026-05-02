package com.shivamingale.ecom.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.shivamingale.ecom.dto.response.AppResponse;
import com.shivamingale.ecom.dto.response.AppResponse.ErrorDetail;
import com.shivamingale.ecom.dto.response.AppResponse.FieldError;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public AppResponse<Void> handleAppException(AppException ex, HttpServletRequest request) {
        log.warn("[{}] at {}: {}", ex.getStatus(), request.getRequestURI(), ex.getMessage());
        return AppResponse.error(
                ex.getMessage(),
                ex.getStatus(),
                ErrorDetail.builder()
                        .code("APP_ERROR")
                        .details(ex.getMessage())
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AppResponse<Void> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .rejectedValue(fe.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        log.warn("Validation failed at {}: {} field errors", request.getRequestURI(), fieldErrors.size());
        return AppResponse.error(
                "Request body failed validation",
                HttpStatus.BAD_REQUEST,
                ErrorDetail.builder()
                        .code("VALIDATION_FAILED")
                        .details(fieldErrors)
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    @ExceptionHandler(BindException.class)
    public AppResponse<Void> handleBindException(BindException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getFieldErrors().stream()
                .map(fe -> FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .rejectedValue(fe.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        return AppResponse.error(
                "Request binding failed",
                HttpStatus.BAD_REQUEST,
                ErrorDetail.builder()
                        .code("VALIDATION_FAILED")
                        .details(fieldErrors)
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public AppResponse<Void> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "Data integrity violation";
        List<FieldError> fieldErrors = null;

        Throwable cause = ex.getCause();
        if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintEx) {
            String constraintName = constraintEx.getConstraintName();
            if (constraintName != null) {
                String columnName = extractColumnFromConstraint(constraintName);
                String sqlState = constraintEx.getSQLState();
                if (sqlState != null && sqlState.equals("23502")) {
                    message = "Required field cannot be null: " + columnName;
                    fieldErrors = List.of(FieldError.builder()
                            .field(columnName)
                            .message("This field is required")
                            .rejectedValue(null)
                            .build());
                } else if (sqlState != null && sqlState.equals("23505")) {
                    message = "Duplicate value for field: " + columnName;
                    fieldErrors = List.of(FieldError.builder()
                            .field(columnName)
                            .message("A record with this value already exists")
                            .rejectedValue(null)
                            .build());
                }
            }
        }

        log.warn("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMessage());
        return AppResponse.error(
                message,
                HttpStatus.BAD_REQUEST,
                ErrorDetail.builder()
                        .code("DATA_CONFLICT")
                        .details(fieldErrors)
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public AppResponse<Void> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {} [traceId={}]", request.getRequestURI(), getTraceId(), ex);
        return AppResponse.error(
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorDetail.builder()
                        .code("INTERNAL_ERROR")
                        .details(ex.getMessage())
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null) ? traceId : "N/A";
    }

    private String extractColumnFromConstraint(String constraintName) {
        if (constraintName == null) return "unknown";
        if (constraintName.contains("_")) {
            String[] parts = constraintName.split("_");
            for (int i = parts.length - 1; i >= 0; i--) {
                String part = parts[i].toLowerCase();
                if (!part.equals("key") && !part.equals("idx") && !part.equals("fkey") && part.length() > 2) {
                    return part;
                }
            }
        }
        return constraintName;
    }
}
