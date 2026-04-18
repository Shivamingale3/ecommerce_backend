package com.shivamingale.invoice.dto.response;

import com.shivamingale.invoice.enums.Role;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String companyName;
    private Role role;
    private Instant createdAt;
}
