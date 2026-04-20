package com.shivamingale.ecom.security;

import java.io.IOException;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {
    private final AdminJwtTokenProvider tokenProvider;

    @Value("${cookie.admin-access-token-name:admin_access_token}")
    private String accessTokenCookieName;

    @Value("${cookie.admin-refresh-token-name:admin_refresh_token}")
    private String refreshTokenCookieName;

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Claims claims = tokenProvider.parseClaims(jwt);
                if (!"admin".equals(claims.get("type", String.class))) {
                    filterChain.doFilter(request, response);
                    return;
                }
                com.shivamingale.ecom.entity.Admin admin = new com.shivamingale.ecom.entity.Admin();
                admin.setId(claims.getSubject());
                admin.setEmail(claims.get("email", String.class));
                admin.setName(claims.get("name", String.class));
                admin.setMobile(claims.get("mobile", String.class));
                admin.setEnabled(true);
                AdminPrincipal principal = new AdminPrincipal(admin);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) { log.warn("Admin auth failed: {}", ex.getMessage()); }
        finally { filterChain.doFilter(request, response); }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String token = Arrays.stream(cookies).filter(c -> accessTokenCookieName.equals(c.getName())).findFirst().map(Cookie::getValue).orElse(null);
            if (StringUtils.hasText(token)) return token;
        }
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) return bearer.substring(7);
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/admin");
    }
}