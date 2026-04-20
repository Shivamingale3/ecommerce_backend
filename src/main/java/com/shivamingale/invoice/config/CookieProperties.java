package com.shivamingale.invoice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {

    private String accessTokenName = "access_token";
    private String refreshTokenName = "refresh_token";
    private String path = "/";
    private int accessTokenMaxAge = 86400;
    private int refreshTokenMaxAge = 604800;
    private boolean httpOnly = true;
    private boolean secure = true;
    private String sameSite = "Strict";
}
