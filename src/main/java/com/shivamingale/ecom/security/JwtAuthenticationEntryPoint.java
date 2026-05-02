package com.shivamingale.ecom.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivamingale.ecom.dto.response.AppResponse;
import com.shivamingale.ecom.dto.response.AppResponse.ErrorDetail;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        AppResponse<Void> errorResponse = AppResponse.error(
                "Authentication required. Please provide a valid Bearer token.",
                HttpStatus.UNAUTHORIZED,
                ErrorDetail.builder()
                        .code("UNAUTHORIZED")
                        .details(authException.getMessage())
                        .path(request.getRequestURI())
                        .traceId(null)
                        .build());

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
