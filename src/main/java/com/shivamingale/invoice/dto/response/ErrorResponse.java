package com.shivamingale.invoice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private String traceId;
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
