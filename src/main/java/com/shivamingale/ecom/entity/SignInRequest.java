package com.shivamingale.ecom.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sign_in_requests")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class SignInRequest extends BaseEntity {
    @Column(nullable = false)
    @Email
    private String email;

    @Column(nullable = false)
    private String otp;

    @Column(nullable = false)
    @Builder.Default
    private Instant validTill = Instant.now().plusSeconds(5 * 60);
}
