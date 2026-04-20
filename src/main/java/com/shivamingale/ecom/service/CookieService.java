package com.shivamingale.ecom.service;

import com.shivamingale.ecom.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookieService {

    private final CookieProperties cookieProperties;

    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        setCookie(response,
                cookieProperties.getAccessTokenName(),
                token,
                cookieProperties.getAccessTokenMaxAge());
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        setCookie(response,
                cookieProperties.getRefreshTokenName(),
                token,
                cookieProperties.getRefreshTokenMaxAge());
    }

    public void clearAccessTokenCookie(HttpServletResponse response) {
        setCookie(response,
                cookieProperties.getAccessTokenName(),
                "",
                0);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        setCookie(response,
                cookieProperties.getRefreshTokenName(),
                "",
                0);
    }

    public void clearAllTokenCookies(HttpServletResponse response) {
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
    }

    public Cookie getAccessTokenCookie(Cookie[] cookies) {
        return getCookie(cookies, cookieProperties.getAccessTokenName());
    }

    public Cookie getRefreshTokenCookie(Cookie[] cookies) {
        return getCookie(cookies, cookieProperties.getRefreshTokenName());
    }

    public String getAccessToken(Cookie[] cookies) {
        Cookie cookie = getAccessTokenCookie(cookies);
        return cookie != null ? cookie.getValue() : null;
    }

    public String getRefreshToken(Cookie[] cookies) {
        Cookie cookie = getRefreshTokenCookie(cookies);
        return cookie != null ? cookie.getValue() : null;
    }

    private void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(cookieProperties.getPath());
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(cookieProperties.isHttpOnly());
        cookie.setSecure(cookieProperties.isSecure());
        cookie.setAttribute("SameSite", cookieProperties.getSameSite());
        response.addCookie(cookie);
        log.debug("Cookie set: {} (maxAge={})", name, maxAge);
    }

    private Cookie getCookie(Cookie[] cookies, String name) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }
}
