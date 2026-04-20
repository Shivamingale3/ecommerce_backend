package com.shivamingale.ecom.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifySignInOtpRequest {

    @NotBlank(message = "Request ID is required")
    private String requestId;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid OTP")
    private String otp;

}
