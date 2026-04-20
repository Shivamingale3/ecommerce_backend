package com.shivamingale.ecom;

import com.shivamingale.ecom.config.EnvValidator;
import com.shivamingale.ecom.config.EnvValidator.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import static com.shivamingale.ecom.config.EnvValidator.logValidationResult;

@Component
public class EnvCheckRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EnvCheckRunner.class);

    private final Environment env;

    public EnvCheckRunner(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Running environment configuration validation...");

        ValidationResult result = EnvValidator.create(env)
                // ==================== DATASOURCE ====================
                .string("spring.datasource.url", true)
                .string("spring.datasource.username", true)
                .secret("spring.datasource.password", true)

                // ==================== MAIL ====================
                .string("spring.mail.host", true)
                .number("spring.mail.port", false)
                .string("spring.mail.username", false)
                .secret("spring.mail.password", false)

                // ==================== JWT ====================
                .secret("jwt.secret", true)

                // ==================== SERVER ====================
                .string("server.port", false)

                // ==================== COOKIE ====================
                .string("cookie.access-token-name", false)
                .string("cookie.refresh-token-name", false)
                .string("cookie.secure", false)
                .number("cookie.access-token-max-age", false)
                .number("cookie.refresh-token-max-age", false)

                // ==================== INVOICE ====================
                .string("invoice.sequence-format", false)
                .number("invoice.sequence-cache-size", false)

                // ==================== REMINDER ====================
                .string("reminder.cron", false)
                .number("reminder.max-attempts", false)
                .string("reminder.enabled", false)

                // ==================== MAIL CUSTOM ====================
                .string("mail-custom.from", false)
                .string("mail-custom.from-name", false)
                .string("mail-custom.health-check-enabled", false)

                .check();

        // Log in dev profile
        logValidationResult(result, log);

        if (!result.valid()) {
            log.error("❌ Environment validation failed! Fix the errors above before proceeding.");
            throw new IllegalStateException(
                    "Environment validation failed with " + result.errors().size() + " error(s): " +
                            result.errors().stream()
                                    .map(e -> e.path() + " — " + e.message())
                                    .reduce((a, b) -> a + "; " + b)
                                    .orElse(""));
        }

        log.info("✅ Environment configuration is valid.");
    }
}