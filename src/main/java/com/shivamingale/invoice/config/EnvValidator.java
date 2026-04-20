package com.shivamingale.invoice.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

public class EnvValidator {

    private static final Logger log = LoggerFactory.getLogger(EnvValidator.class);

    private final List<ValidationError> errors = new ArrayList<>();
    private final Map<String, Object> capturedValues = new HashMap<>();
    private final Environment env;
    private final String profile;

    public record ValidationError(String path, String message) {
    }

    public record Issue(String path, String message, String severity) {
    }

    private EnvValidator(Environment env, String profile) {
        this.env = env;
        this.profile = profile;
    }

    public static EnvValidator create(Environment env) {
        return new EnvValidator(env, getActiveProfile(env));
    }

    private static String getActiveProfile(Environment env) {
        String[] active = env.getActiveProfiles();
        return active.length > 0 ? active[0] : "default";
    }

    // ==================== PRIMITIVES ====================

    public EnvValidator string(String path, boolean required) {
        String value = env.getProperty(path);
        capturedValues.put(path, value != null ? value : "");
        if (required && isBlank(value)) {
            errors.add(new ValidationError(path, "required"));
        }
        return this;
    }

    public EnvValidator number(String path, boolean required) {
        String value = env.getProperty(path);
        capturedValues.put(path, value != null ? value : "");
        if (required && isBlank(value)) {
            errors.add(new ValidationError(path, "required"));
        } else if (!isBlank(value)) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                errors.add(new ValidationError(path, "must be a valid number"));
            }
        }
        return this;
    }

    public EnvValidator boolean_(String path, boolean required) {
        String value = env.getProperty(path);
        capturedValues.put(path, value != null ? value : "");
        if (required && isBlank(value)) {
            errors.add(new ValidationError(path, "required"));
        } else if (!isBlank(value)) {
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false") && !value.equalsIgnoreCase("yes")
                    && !value.equalsIgnoreCase("no")) {
                errors.add(new ValidationError(path, "must be 'true' or 'false'"));
            }
        }
        return this;
    }

    // ==================== CUSTOM VALIDATORS ====================

    public EnvValidator validate(String path, Function<String, String> validator, String errorMessage) {
        String value = env.getProperty(path);
        capturedValues.put(path, value != null ? value : "");
        if (value != null) {
            String error = validator.apply(value);
            if (error != null) {
                errors.add(new ValidationError(path, errorMessage + ": " + error));
            }
        }
        return this;
    }

    public EnvValidator secret(String path, boolean required) {
        return validate(path,
                value -> {
                    if (value.contains("change_me") || value.contains("example") || value.length() < 32) {
                        return "appears to be a placeholder or weak secret";
                    }
                    return null;
                },
                "secret validation failed");
    }

    // ==================== RESULT ====================

    public record ValidationResult(
            boolean valid,
            List<ValidationError> errors,
            Map<String, Object> capturedValues,
            String profile) {
    }

    public ValidationResult check() {
        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, new ArrayList<>(errors), new HashMap<>(capturedValues), profile);
    }

    // ==================== UTILS ====================

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ==================== LOGGING (dev only) ====================

    public static void logValidationResult(ValidationResult result, Logger logger) {
        if (result.profile().equals("dev")) {
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            logger.info("  ENVIRONMENT CONFIGURATION");
            logger.info("  Profile: {}", result.profile());
            logger.info("  Status: {}", result.valid() ? "✅ VALID" : "❌ INVALID");
            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            for (Map.Entry<String, Object> entry : result.capturedValues().entrySet()) {
                String value = maskIfSensitive(entry.getKey(),
                        entry.getValue() != null ? entry.getValue().toString() : "");
                logger.info("  {} = {}", entry.getKey(), value);
            }

            if (!result.errors().isEmpty()) {
                logger.warn("  Validation Errors:");
                for (ValidationError err : result.errors()) {
                    logger.warn("    ❌ {} — {}", err.path(), err.message());
                }
            }

            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    private static String maskIfSensitive(String path, String value) {
        if (path.toLowerCase().contains("password") ||
                path.toLowerCase().contains("secret") ||
                path.toLowerCase().contains("token") && path.toLowerCase().contains("key")) {
            if (isBlank(value))
                return "[not set]";
            if (value.length() <= 8)
                return "******";
            return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
        }
        return isBlank(value) ? "[not set]" : value;
    }
}