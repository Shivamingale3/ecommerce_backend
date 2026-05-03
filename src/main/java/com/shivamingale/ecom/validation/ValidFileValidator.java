package com.shivamingale.ecom.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class ValidFileValidator implements ConstraintValidator<ValidFile, MultipartFile> {

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Media file is required")
                    .addConstraintViolation();
            return false;
        }

        if (file.isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Media file cannot be empty")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
