package com.shivamingale.invoice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivamingale.invoice.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse error =
                ErrorResponse.builder()
                        .status(HttpServletResponse.SC_UNAUTHORIZED)
                        .error("Unauthorized")
                        .message("Authentication required. Please provide a valid Bearer token.")
                        .path(request.getRequestURI())
                        .timestamp(Instant.now())
                        .build();

        objectMapper.findAndRegisterModules();
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
