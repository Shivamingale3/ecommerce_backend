package com.shivamingale.ecom.config;

import com.shivamingale.ecom.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObjectStorageHealthIndicator implements ApplicationRunner {

    private final ObjectStorageService objectStorageService;
    private final ObjectStorageProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Object storage is disabled, skipping health check");
            return;
        }

        log.info("Checking object storage connection to {}...", properties.getEndpoint());

        try {
            if (objectStorageService.isConnected()) {
                log.info("Object storage health check PASSED - bucket '{}' is accessible",
                        properties.getBucket());
            } else {
                log.warn("Object storage health check - bucket '{}' not found or not accessible. " +
                        "Bucket will be created on first upload.",
                        properties.getBucket());
            }
        } catch (Exception e) {
            log.error("Object storage health check FAILED - could not connect to {}: {}",
                    properties.getEndpoint(), e.getMessage());
            throw new RuntimeException("Object storage connection failed: " + e.getMessage(), e);
        }
    }

    public boolean isHealthy() {
        if (!properties.isEnabled()) {
            return true;
        }
        return objectStorageService.isConnected();
    }
}