package com.shivamingale.ecom.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivamingale.ecom.dto.request.SignInRequestDto;
import com.shivamingale.ecom.dto.request.VerifySignInOtpRequest;
import com.shivamingale.ecom.dto.response.UserResponse;
import com.shivamingale.ecom.entity.SignInRequest;
import com.shivamingale.ecom.entity.User;
import com.shivamingale.ecom.exception.AppException;
import com.shivamingale.ecom.repository.SignInRequestRepository;
import com.shivamingale.ecom.security.JwtTokenProvider;
import com.shivamingale.ecom.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider tokenProvider;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private SignInRequestRepository signInRequestRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public Map<String, String> verifySignInOtp(VerifySignInOtpRequest request) {
        SignInRequest signInRequest = signInRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Invalid Request ID", null));

        if (signInRequest.getValidTill().isBefore(Instant.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "OTP has expired", null);
        }

        if (!signInRequest.getOtp().equals(request.getOtp())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Invalid OTP", null);
        }

        User user = userService.getUserByEmail(signInRequest.getEmail())
                .orElseGet(() -> userService.registerNewUser(signInRequest.getEmail(), null, null));

        signInRequestRepository.delete(signInRequest);

        return Map.of("accessToken", tokenProvider.generateAccessToken(user), "refreshToken",
                tokenProvider.generateRefreshToken(user));
    }

    @Transactional
    public Map<String, String> requestSignInOtp(SignInRequestDto request) {

        Optional<SignInRequest> existingRequest = signInRequestRepository.findByEmailAndDeletedFalse(request.getEmail());

        if (existingRequest.isPresent()) {
            if (existingRequest.get().getValidTill().isAfter(Instant.now())) {
                return Map.of("requestId", existingRequest.get().getId(), "validTill",
                        existingRequest.get().getValidTill().toString());
            }
            signInRequestRepository.delete(existingRequest.get());
        }

        String otp = generateOTP();
        SignInRequest signInRequest = signInRequestRepository.save(SignInRequest.builder()
                .email(request.getEmail())
                .otp(otp)
                .build());
        try {
            emailTemplateService.sendOtpEmail(request.getEmail(), "User", otp, 5);
        } catch (Exception e) {
            log.warn("Failed to send OTP email to {}: {}. OTP is: {}", request.getEmail(), e.getMessage(), otp);
        }
        log.info("{} has requested to sign in! OTP stored: {}", request.getEmail(), otp);
        return Map.of("requestId", signInRequest.getId(), "validTill", signInRequest.getValidTill().toString());
    }

    @Transactional(readOnly = true)
    public Map<String, String> refresh(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token", null);
        }

        String userId = tokenProvider.getUserId(refreshToken);
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid Refresh Token", null));

        if (!user.isEnabled()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Your account has been disabled", null);
        }

        return Map.of("accessToken", tokenProvider.generateAccessToken(user), "refreshToken",
                tokenProvider.generateRefreshToken(user));
    }

    public UserResponse getCurrentUser(UserPrincipal principal) {
        return UserResponse.builder()
                .id(principal.getId())
                .email(principal.getEmail())
                .firstName(principal.getFirstName())
                .lastName(principal.getLastName())
                .build();
    }

    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
