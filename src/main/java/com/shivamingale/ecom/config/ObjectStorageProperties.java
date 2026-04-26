package com.shivamingale.ecom.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "object-storage")
public class ObjectStorageProperties {

    private boolean enabled = true;
    private String endpoint = "http://localhost:9000";
    private String region = "us-east-1";
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String publicUrlPrefix;
    private int connectionTimeout = 10_000;
    private int readTimeout = 30_000;
}