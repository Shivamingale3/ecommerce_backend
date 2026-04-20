package com.shivamingale.ecom.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
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
    private com.shivamingale.ecom.service.EmailTemplateService emailTemplateService;

    @Transactional
    public Map<String, String> requestSignInOtp(String email) {
        Optional<AdminSignInRequest> existing = signInRequestRepository.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().getValidTill().isAfter(Instant.now())) {
                return Map.of("requestId", existing.get().getId(), "validTill", existing.get().getValidTill().toString());
            }
            signInRequestRepository.delete(existing.get());
        }
        String otp = String.valueOf((int) (Math.random() * 899999) + 100000);
        AdminSignInRequest req = signInRequestRepository.save(AdminSignInRequest.builder().email(email).otp(otp).build());
        try { emailTemplateService.sendOtpEmail(email, "Admin", otp, 5); }
        catch (Exception e) { log.warn("Failed to send admin OTP to {}: {}. OTP: {}", email, e.getMessage(), otp); }
        log.info("Admin OTP requested for {}: {}", email, otp);
        return Map.of("requestId", req.getId(), "validTill", req.getValidTill().toString());
    }

    @Transactional
    public Map<String, String> verifySignInOtp(String requestId, String otp) {
        AdminSignInRequest req = signInRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Invalid Request ID", null));
        if (req.getValidTill().isBefore(Instant.now())) throw new AppException(HttpStatus.BAD_REQUEST, "OTP has expired", null);
        if (!req.getOtp().equals(otp)) throw new AppException(HttpStatus.FORBIDDEN, "Invalid OTP", null);
        Admin admin = adminRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Admin not found", null));
        if (!admin.isEnabled()) throw new AppException(HttpStatus.FORBIDDEN, "Admin account is disabled", null);
        signInRequestRepository.delete(req);
        return Map.of("accessToken", tokenProvider.generateAccessToken(admin), "refreshToken", tokenProvider.generateRefreshToken(admin));
    }

    @Transactional(readOnly = true)
    public Map<String, String> refresh(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token", null);
        String adminId = tokenProvider.getUserId(refreshToken);
        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid Refresh Token", null));
        if (!admin.isEnabled()) throw new AppException(HttpStatus.FORBIDDEN, "Admin account is disabled", null);
        return Map.of("accessToken", tokenProvider.generateAccessToken(admin), "refreshToken", tokenProvider.generateRefreshToken(admin));
    }
}