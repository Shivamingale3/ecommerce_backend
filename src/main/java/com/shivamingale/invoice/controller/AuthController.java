package com.shivamingale.invoice.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivamingale.invoice.dto.request.RefreshTokenRequest;
import com.shivamingale.invoice.dto.request.SignInRequestDto;
import com.shivamingale.invoice.dto.request.VerifySignInOtpRequest;
import com.shivamingale.invoice.dto.response.AppResponse;
import com.shivamingale.invoice.dto.response.AuthResponse;
import com.shivamingale.invoice.dto.response.UserResponse;
import com.shivamingale.invoice.security.UserPrincipal;
import com.shivamingale.invoice.service.AuthService;
import com.shivamingale.invoice.service.CookieService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CookieService cookieService;

    @PostMapping("/request-otp")
    public ResponseEntity<AppResponse<Map<String, String>>> requestSignInOtp(
            @Valid @RequestBody SignInRequestDto request) {
        String requestId = authService.requestSignInOtp(request);
        return ResponseEntity
                .ok(AppResponse.success(Map.of("requestId", requestId), "OTP sent successfully", HttpStatus.OK));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AppResponse<AuthResponse>> verifySignInOtp(
            @Valid @RequestBody VerifySignInOtpRequest request, HttpServletResponse response) {
        Map<String, String> tokens = authService.verifySignInOtp(request);
        cookieService.setAccessTokenCookie(response, tokens.get("accessToken"));
        cookieService.setRefreshTokenCookie(response, tokens.get("refreshToken"));
        return ResponseEntity
                .ok(AppResponse.success(null, "OTP Verified! Signed in successfully", HttpStatus.OK));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AppResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request,
            HttpServletResponse response) {
        Map<String, String> tokens = authService.refresh(request);
        cookieService.setAccessTokenCookie(response, tokens.get("accessToken"));
        cookieService.setRefreshTokenCookie(response, tokens.get("refreshToken"));
        return ResponseEntity
                .ok(AppResponse.success(null, "Token refreshed successfully", HttpStatus.OK));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal));
    }
}
