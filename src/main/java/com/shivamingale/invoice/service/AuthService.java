package com.shivamingale.invoice.service;

import com.shivamingale.invoice.config.JwtProperties;
import com.shivamingale.invoice.dto.request.LoginRequest;
import com.shivamingale.invoice.dto.request.RefreshTokenRequest;
import com.shivamingale.invoice.dto.request.RegisterRequest;
import com.shivamingale.invoice.dto.response.AuthResponse;
import com.shivamingale.invoice.dto.response.UserResponse;
import com.shivamingale.invoice.entity.Tenant;
import com.shivamingale.invoice.entity.User;
import com.shivamingale.invoice.enums.Role;
import com.shivamingale.invoice.repository.TenantRepository;
import com.shivamingale.invoice.repository.UserRepository;
import com.shivamingale.invoice.security.JwtTokenProvider;
import com.shivamingale.invoice.security.UserPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // Create tenant for the new organization
        Tenant tenant =
                Tenant.builder()
                        .name(request.getCompanyName())
                        .ownerEmail(request.getEmail())
                        .build();
        tenant = tenantRepository.save(tenant);

        // Create the first admin user for this tenant
        User user =
                User.builder()
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .role(Role.ADMIN)
                        .tenant(tenant)
                        .enabled(true)
                        .accountNonLocked(true)
                        .build();
        user = userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        log.info("New user registered: {} [tenant={}]", user.getEmail(), tenant.getName());

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId()).orElseThrow();

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        UUID userId = tokenProvider.getUserId(request.getRefreshToken());
        User user = userRepository.findById(userId).orElseThrow();

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    public UserResponse getCurrentUser(UserPrincipal principal) {
        return UserResponse.builder()
                .id(principal.getId())
                .email(principal.getEmail())
                .firstName(principal.getFirstName())
                .lastName(principal.getLastName())
                .role(principal.getRole())
                .build();
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs() / 1000)
                .user(
                        UserResponse.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .companyName(user.getTenant().getName())
                                .role(user.getRole())
                                .createdAt(user.getCreatedAt())
                                .build())
                .build();
    }
}
