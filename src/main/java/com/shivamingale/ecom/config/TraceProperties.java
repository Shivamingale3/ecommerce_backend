package com.shivamingale.ecom.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.trace")
public class TraceProperties {

    private boolean enabled = false;
}
