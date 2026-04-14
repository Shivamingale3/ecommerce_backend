package com.shivamingale.invoice.dto.response;

import com.shivamingale.invoice.enums.Role;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String companyName;
    private Role role;
    private Instant createdAt;
}
