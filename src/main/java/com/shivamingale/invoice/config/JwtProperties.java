package com.shivamingale.invoice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "change_me_you_must_set_a_256_bit_secret_minimum_in_production";
    private long expirationMs = 86400000;
    private long refreshExpirationMs = 604800000;
}
