package com.shivamingale.invoice.dto.response;

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
    private Object error;

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

    public static <T> AppResponse<T> error(String message, HttpStatus status, Object error) {
        return AppResponse.<T>builder()
                .success(false)
                .status(status)
                .message(message)
                .timestamp(Instant.now())
                .error(error)
                .build();
    }
}
