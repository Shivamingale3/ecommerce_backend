package com.shivamingale.ecom;

import com.shivamingale.ecom.config.MailProperties;
import com.shivamingale.ecom.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MailHealthCheckRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MailHealthCheckRunner.class);

    private final MailService mailService;
    private final MailProperties mailProperties;

    public MailHealthCheckRunner(MailService mailService, MailProperties mailProperties) {
        this.mailService = mailService;
        this.mailProperties = mailProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!mailProperties.isHealthCheckEnabled()) {
            log.info("Mail service health check is disabled, skipping");
            return;
        }
        try {
            log.info("Running mail service health check...");
            mailService.checkConnection();
            log.info("Mail service health check passed");
        } catch (Exception e) {
            log.warn("Mail service health check failed: {}. Application will continue but email features may not work.",
                    e.getMessage());
        }
    }
}
