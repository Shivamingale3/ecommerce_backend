package com.shivamingale.ecom.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivamingale.ecom.dto.request.RefreshTokenRequest;
import com.shivamingale.ecom.dto.request.SignInRequestDto;
import com.shivamingale.ecom.dto.request.VerifySignInOtpRequest;
import com.shivamingale.ecom.dto.response.AppResponse;
import com.shivamingale.ecom.dto.response.AuthResponse;
import com.shivamingale.ecom.dto.response.UserResponse;
import com.shivamingale.ecom.exception.AppException;
import com.shivamingale.ecom.security.UserPrincipal;
import com.shivamingale.ecom.service.AuthService;
import com.shivamingale.ecom.service.CookieService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CookieService cookieService;

    @Value("${cookie.refresh-token-name:refresh_token}")
    private String refreshTokenCookieName;

    @PostMapping("/request-otp")
    public ResponseEntity<AppResponse<Map<String, String>>> requestSignInOtp(
            @Valid @RequestBody SignInRequestDto request) {
        Map<String, String> result = authService.requestSignInOtp(request);
        return ResponseEntity
                .ok(AppResponse.success(result, "OTP sent successfully", HttpStatus.OK));
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
    public ResponseEntity<AppResponse<AuthResponse>> refresh(
            @Valid @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshToken(request, requestBody);

        Map<String, String> tokens = authService.refresh(refreshToken);
        cookieService.setAccessTokenCookie(response, tokens.get("accessToken"));
        cookieService.setRefreshTokenCookie(response, tokens.get("refreshToken"));
        return ResponseEntity
                .ok(AppResponse.success(null, "Token refreshed successfully", HttpStatus.OK));
    }

    private String extractRefreshToken(HttpServletRequest request, RefreshTokenRequest requestBody) {
        // First check cookies for refresh token
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (refreshTokenCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Fall back to request body
        if (requestBody != null && requestBody.getRefreshToken() != null) {
            return requestBody.getRefreshToken();
        }

        throw new AppException(HttpStatus.BAD_REQUEST, "Refresh token is required", null);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal));
    }
}
