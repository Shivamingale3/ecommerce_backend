package com.shivamingale.ecom.dto.response;

import java.time.Instant;

import org.springframework.http.HttpStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppResponse<T> {
    private boolean success;
    private HttpStatus status;
    private String message;
    private Instant timestamp;
    private T data;
    private ErrorDetail error;

    public static <T> AppResponse<T> success(T data, String message, HttpStatus status) {
        return AppResponse.<T>builder()
                .success(true)
                .status(status)
                .message(message)
                .timestamp(Instant.now())
                .data(data)
                .error(null)
                .build();
    }

    public static <T> AppResponse<T> error(String message, HttpStatus status, ErrorDetail error) {
        return AppResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .timestamp(Instant.now())
                .data(null)
                .error(error)
                .build();
    }

    @Data
    @Builder
    public static class ErrorDetail {
        private String code;
        private Object details;
        private String path;
        private String traceId;
    }

    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    @Data
    @Builder
    public static class ErrorDetailInfo {
        private String messagingError;
        private String failingAddress;
        private String resource;
        private String exceptionType;
        private String nullField;
    }
}
