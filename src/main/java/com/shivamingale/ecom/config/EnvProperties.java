package com.shivamingale.ecom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class EnvProperties {

    // ==================== DATASOURCE ====================
    private String springDatasourceUrl;
    private String springDatasourceUsername;
    private String springDatasourcePassword;

    // ==================== MAIL ====================
    private String springMailHost;
    private String springMailPort;
    private String springMailUsername;
    private String springMailPassword;

    // ==================== JWT ====================
    private String jwtSecret;
    private String jwtExpirationMs;
    private String jwtRefreshExpirationMs;

    // ==================== SERVER ====================
    private String serverPort;

    // ==================== COOKIE ====================
    private String cookieAccessTokenName;
    private String cookieRefreshTokenName;
    private String cookieAccessTokenMaxAge;
    private String cookieRefreshTokenMaxAge;
    private String cookieSecure;

    // ==================== INVOICE ====================
    private String invoiceSequenceFormat;
    private String invoiceSequenceCacheSize;

    // ==================== REMINDER ====================
    private String reminderCron;
    private String reminderMaxAttempts;
    private String reminderEnabled;

    // ==================== MAIL CUSTOM ====================
    private String mailFrom;
    private String mailFromName;
    private String mailCustomHealthCheckEnabled;

    // ==================== CORS ====================
    private String corsAllowedOrigins;
    private String corsAllowedMethods;
    private String corsAllowedHeaders;
    private Boolean corsAllowCredentials;
}