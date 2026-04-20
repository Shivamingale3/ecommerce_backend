package com.shivamingale.ecom.security;

import com.shivamingale.ecom.config.JwtProperties;
import com.shivamingale.ecom.entity.Admin;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminJwtTokenProvider {
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Admin admin) {
        return buildToken(admin, jwtProperties.getExpirationMs());
    }

    public String generateRefreshToken(Admin admin) {
        return buildToken(admin, jwtProperties.getRefreshExpirationMs());
    }

    private String buildToken(Admin admin, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(admin.getId())
                .claim("email", admin.getEmail())
                .claim("name", admin.getName())
                .claim("mobile", admin.getMobile())
                .claim("type", "admin")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try { parseClaims(token); return true; }
        catch (SignatureException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("Admin JWT invalid: {}", e.getMessage()); return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    public String getUserId(String token) { return parseClaims(token).getSubject(); }

    public String getEmail(String token) { return parseClaims(token).get("email", String.class); }

    public String getType(String token) { return parseClaims(token).get("type", String.class); }
}