package com.shivamingale.ecom.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private HttpStatus status;
    private String error;
    private String message;
    private String path;
    private String traceId;
    private String trace;
    private Instant timestamp;
    private List<FieldError> fieldErrors;

    @Data
    @Builder
    @Jacksonized
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
