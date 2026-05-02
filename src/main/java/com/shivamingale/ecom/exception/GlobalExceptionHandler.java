package com.shivamingale.ecom.exception;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.shivamingale.ecom.dto.response.AppResponse;
import com.shivamingale.ecom.dto.response.AppResponse.ErrorDetail;
import com.shivamingale.ecom.dto.response.AppResponse.ErrorDetailInfo;
import com.shivamingale.ecom.dto.response.AppResponse.FieldError;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import software.amazon.awssdk.core.exception.SdkException;

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

        Throwable root = getRootCause(ex);
        if (root instanceof org.hibernate.exception.ConstraintViolationException constraintEx) {
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
                } else if (sqlState != null && sqlState.equals("23503")) {
                    message = "Referenced record does not exist: " + columnName;
                    fieldErrors = List.of(FieldError.builder()
                            .field(columnName)
                            .message("Referenced record does not exist")
                            .rejectedValue(null)
                            .build());
                } else {
                    message = "Database constraint violation on: " + columnName;
                    fieldErrors = List.of(FieldError.builder()
                            .field(columnName)
                            .message(constraintEx.getMessage())
                            .rejectedValue(null)
                            .build());
                }
            }
        } else if (root instanceof SQLException sqlEx) {
            message = "Database error";
            fieldErrors = List.of(FieldError.builder()
                    .field(extractColumnFromConstraint(sqlEx.getSQLState()))
                    .message(getRootCauseMessage(sqlEx))
                    .rejectedValue(null)
                    .build());
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

    @ExceptionHandler(MessagingException.class)
    public AppResponse<Void> handleMessagingException(MessagingException ex, HttpServletRequest request) {
        String rootMessage = getRootCauseMessage(ex);
        String failingAddress = extractFailingAddress(ex);

        log.error("Mail error at {}: {}", request.getRequestURI(), rootMessage);
        return AppResponse.error(
                "Failed to send email",
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorDetail.builder()
                        .code("EXTERNAL_SERVICE_ERROR")
                        .details(ErrorDetailInfo.builder()
                                .messagingError(rootMessage)
                                .failingAddress(failingAddress)
                                .exceptionType(ex.getClass().getSimpleName())
                                .build())
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    @ExceptionHandler(SdkException.class)
    public AppResponse<Void> handleS3Exception(SdkException ex, HttpServletRequest request) {
        String rootMessage = getRootCauseMessage(ex);
        String resource = extractS3Resource(ex);

        log.error("Object storage error at {}: {}", request.getRequestURI(), rootMessage);
        return AppResponse.error(
                "Object storage operation failed",
                HttpStatus.SERVICE_UNAVAILABLE,
                ErrorDetail.builder()
                        .code("EXTERNAL_SERVICE_ERROR")
                        .details(ErrorDetailInfo.builder()
                                .messagingError(rootMessage)
                                .resource(resource)
                                .exceptionType(ex.getClass().getSimpleName())
                                .build())
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    @ExceptionHandler(IOException.class)
    public AppResponse<Void> handleIOException(IOException ex, HttpServletRequest request) {
        String rootMessage = getRootCauseMessage(ex);
        boolean isTimeout = ex instanceof SocketTimeoutException
                || rootMessage.contains("Connection refused")
                || rootMessage.contains("timeout");

        if (isTimeout) {
            log.error("Connection error at {}: {}", request.getRequestURI(), rootMessage);
            return AppResponse.error(
                    "Service temporarily unavailable",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ErrorDetail.builder()
                            .code("EXTERNAL_SERVICE_ERROR")
                            .details(ErrorDetailInfo.builder()
                                    .messagingError(rootMessage)
                                    .exceptionType(ex.getClass().getSimpleName())
                                    .build())
                            .path(request.getRequestURI())
                            .traceId(getTraceId())
                            .build());
        }

        log.error("IO error at {}: {}", request.getRequestURI(), rootMessage);
        return AppResponse.error(
                "File operation failed",
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorDetail.builder()
                        .code("INTERNAL_ERROR")
                        .details(ErrorDetailInfo.builder()
                                .messagingError(rootMessage)
                                .exceptionType(ex.getClass().getSimpleName())
                                .build())
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public AppResponse<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("File upload too large at {}: {}", request.getRequestURI(), ex.getMessage());
        return AppResponse.error(
                "File size exceeds maximum allowed limit",
                HttpStatus.PAYLOAD_TOO_LARGE,
                ErrorDetail.builder()
                        .code("PAYLOAD_TOO_LARGE")
                        .details(ErrorDetailInfo.builder()
                                .messagingError(getRootCauseMessage(ex))
                                .exceptionType("MaxUploadSizeExceededException")
                                .build())
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    @ExceptionHandler(NullPointerException.class)
    public AppResponse<Void> handleNullPointerException(NullPointerException ex, HttpServletRequest request) {
        String rootMessage = getRootCauseMessage(ex);
        String nullField = extractNullFieldFromStackTrace(ex);

        log.error("NullPointerException at {}: {}", request.getRequestURI(), rootMessage);
        return AppResponse.error(
                "A required field is missing or null",
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorDetail.builder()
                        .code("INTERNAL_ERROR")
                        .details(ErrorDetailInfo.builder()
                                .messagingError(rootMessage)
                                .nullField(nullField)
                                .exceptionType("NullPointerException")
                                .build())
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public AppResponse<Void> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());
        return AppResponse.error(
                "Invalid argument provided",
                HttpStatus.BAD_REQUEST,
                ErrorDetail.builder()
                        .code("VALIDATION_FAILED")
                        .details(ErrorDetailInfo.builder()
                                .messagingError(getRootCauseMessage(ex))
                                .exceptionType("IllegalArgumentException")
                                .build())
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public AppResponse<Void> handleGeneric(Exception ex, HttpServletRequest request) {
        String rootMessage = getRootCauseMessage(ex);

        log.error("Unhandled exception at {} [traceId={}]: {}", request.getRequestURI(), getTraceId(), rootMessage, ex);
        return AppResponse.error(
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorDetail.builder()
                        .code("INTERNAL_ERROR")
                        .details(ErrorDetailInfo.builder()
                                .messagingError(rootMessage)
                                .exceptionType(ex.getClass().getSimpleName())
                                .build())
                        .path(request.getRequestURI())
                        .traceId(getTraceId())
                        .build());
    }

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null) ? traceId : "N/A";
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private String getRootCauseMessage(Throwable throwable) {
        Throwable root = getRootCause(throwable);
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
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

    private String extractFailingAddress(MessagingException ex) {
        String message = getRootCauseMessage(ex);
        if (message.contains("@")) {
            int atIndex = message.indexOf('@');
            int start = Math.max(0, atIndex - 5);
            int end = Math.min(message.length(), atIndex + 20);
            return message.substring(start, end);
        }
        return null;
    }

    private String extractS3Resource(SdkException ex) {
        String message = getRootCauseMessage(ex);
        if (message.contains("key")) {
            return message;
        }
        return null;
    }

    private String extractNullFieldFromStackTrace(NullPointerException ex) {
        for (StackTraceElement element : ex.getStackTrace()) {
            String method = element.getMethodName();
            if (method.contains("get") || method.contains("set") || method.contains("invoke")) {
                String className = element.getClassName();
                String simpleClass = className.substring(className.lastIndexOf('.') + 1);
                return simpleClass + "." + method;
            }
        }
        return null;
    }
}
