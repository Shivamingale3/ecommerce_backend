package com.shivamingale.ecom.controller.admin;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shivamingale.ecom.dto.request.RefreshTokenRequest;
import com.shivamingale.ecom.dto.request.VerifySignInOtpRequest;
import com.shivamingale.ecom.dto.response.AppResponse;
import com.shivamingale.ecom.exception.AppException;
import com.shivamingale.ecom.service.AdminAuthService;
import com.shivamingale.ecom.service.CookieService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService adminAuthService;
    private final CookieService cookieService;

    @PostMapping("/request-otp")
    public ResponseEntity<AppResponse<Map<String, String>>> requestOtp(
            @Valid @RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank())
            throw new AppException(HttpStatus.BAD_REQUEST, "Email is required", null);
        return ResponseEntity.ok(
                AppResponse.success(adminAuthService.requestSignInOtp(email), "OTP sent successfully", HttpStatus.OK));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<AppResponse<Map<String, String>>> resendOtp(
            @Valid @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String requestId = request.get("requestId");
        if (email == null || email.isBlank())
            throw new AppException(HttpStatus.BAD_REQUEST, "Email is required", null);
        if (requestId == null || requestId.isBlank())
            throw new AppException(HttpStatus.BAD_REQUEST, "Request ID is required", null);
        return ResponseEntity.ok(
                AppResponse.success(adminAuthService.resendSignInOtp(email, requestId), "OTP resent successfully",
                        HttpStatus.OK));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AppResponse<Void>> verifyOtp(@Valid @RequestBody VerifySignInOtpRequest request,
            HttpServletResponse response) {
        Map<String, String> tokens = adminAuthService.verifySignInOtp(request.getRequestId(), request.getOtp());
        cookieService.setAdminAccessTokenCookie(response, tokens.get("accessToken"));
        cookieService.setAdminRefreshTokenCookie(response, tokens.get("refreshToken"));
        return ResponseEntity.ok(AppResponse.success(null, "OTP Verified! Signed in successfully", HttpStatus.OK));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AppResponse<Void>> refresh(
            @RequestBody(required = false) RefreshTokenRequest requestBody, HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request, requestBody);
        Map<String, String> tokens = adminAuthService.refresh(refreshToken);
        cookieService.setAdminAccessTokenCookie(response, tokens.get("accessToken"));
        cookieService.setAdminRefreshTokenCookie(response, tokens.get("refreshToken"));
        return ResponseEntity.ok(AppResponse.success(null, "Token refreshed successfully", HttpStatus.OK));
    }

    private String extractRefreshToken(HttpServletRequest request, RefreshTokenRequest requestBody) {
        String name = "admin_refresh_token";
        if (request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if (name.equals(c.getName()))
                    return c.getValue();
            }
        }
        if (requestBody != null && requestBody.getRefreshToken() != null)
            return requestBody.getRefreshToken();
        throw new AppException(HttpStatus.BAD_REQUEST, "Refresh token is required", null);
    }

    @GetMapping("/me")
    public ResponseEntity<AppResponse<UserDetails>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(AppResponse.success(userDetails, "Profile fetched successfully", HttpStatus.OK));
    }

    @PostMapping("/sign-out")
    public ResponseEntity<AppResponse<Void>> signOut(HttpServletRequest request,
            HttpServletResponse response) {
        cookieService.setAdminAccessTokenCookie(response, null);
        cookieService.setAdminRefreshTokenCookie(response, null);
        return ResponseEntity.ok(AppResponse.success(null, "Signed out successfully", HttpStatus.OK));
    }
}