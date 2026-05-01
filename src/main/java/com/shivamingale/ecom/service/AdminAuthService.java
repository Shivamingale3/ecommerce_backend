package com.shivamingale.ecom.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivamingale.ecom.entity.Admin;
import com.shivamingale.ecom.entity.AdminSignInRequest;
import com.shivamingale.ecom.exception.AppException;
import com.shivamingale.ecom.repository.AdminRepository;
import com.shivamingale.ecom.repository.AdminSignInRequestRepository;
import com.shivamingale.ecom.security.AdminJwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final AdminJwtTokenProvider tokenProvider;
    private final AdminRepository adminRepository;
    private final AdminSignInRequestRepository signInRequestRepository;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Transactional
    public Map<String, String> requestSignInOtp(String email) {
        Admin admin = adminRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Admin not found", null));
        if (!admin.isEnabled()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Admin account is disabled", null);
        }

        // 1 email = 1 OTP request — always delete any existing request
        signInRequestRepository.findByEmailAndDeletedFalse(email).ifPresent(signInRequestRepository::delete);

        String otp = String.valueOf((int) (Math.random() * 899999) + 100000);
        AdminSignInRequest req = signInRequestRepository
                .save(AdminSignInRequest.builder().email(email).otp(otp).build());
        try {
            emailTemplateService.sendOtpEmail(email, "Admin", otp, 5);
        } catch (Exception e) {
            log.error("Failed to send admin OTP to {}: {}", email, e.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send OTP email", null);
        }
        log.info("Admin OTP requested for {}", email);
        return Map.of("requestId", req.getId(), "validTill", req.getValidTill().toString(), "email", email);
    }

    @Transactional
    public Map<String, String> resendSignInOtp(String email, String requestId) {
        // Validate the existing request belongs to this email
        AdminSignInRequest existingReq = signInRequestRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Invalid Request ID", null));
        if (!existingReq.getEmail().equals(email)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Invalid Request ID or Email", null);
        }

        // Delete old and create fresh OTP
        return requestSignInOtp(email);
    }

    @Transactional
    public Map<String, String> verifySignInOtp(String requestId, String otp) {
        if (otp == null || otp.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "OTP is required", null);
        }
        AdminSignInRequest req = signInRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Invalid Request ID", null));
        if (req.getValidTill().isBefore(Instant.now()))
            throw new AppException(HttpStatus.BAD_REQUEST, "OTP has expired", null);
        if (!req.getOtp().equals(otp))
            throw new AppException(HttpStatus.FORBIDDEN, "Invalid OTP", null);
        Admin admin = adminRepository.findByEmailAndDeletedFalse(req.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Admin not found", null));
        if (!admin.isEnabled())
            throw new AppException(HttpStatus.FORBIDDEN, "Admin account is disabled", null);
        signInRequestRepository.delete(req);
        return Map.of("accessToken", tokenProvider.generateAccessToken(admin), "refreshToken",
                tokenProvider.generateRefreshToken(admin));
    }

    @Transactional(readOnly = true)
    public Map<String, String> refresh(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken))
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token", null);
        String adminId = tokenProvider.getUserId(refreshToken);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid Refresh Token", null));
        if (!admin.isEnabled())
            throw new AppException(HttpStatus.FORBIDDEN, "Admin account is disabled", null);
        return Map.of("accessToken", tokenProvider.generateAccessToken(admin), "refreshToken",
                tokenProvider.generateRefreshToken(admin));
    }
}