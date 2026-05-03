package com.shivamingale.ecom.entity;

import java.time.Instant;

import com.shivamingale.ecom.enums.MediaType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Media extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType type;

    @Column(nullable = false)
    private String key;

    @Column(name = "presigned_url")
    private String presignedUrl;

    @Column(name = "presigned_url_expires_at")
    private Instant presignedUrlExpiresAt;

    @Column
    private String altText;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column(name = "is_temporary", nullable = false)
    @Builder.Default
    private boolean temporary = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    public boolean hasValidPresignedUrl() {
        return presignedUrl != null
                && presignedUrlExpiresAt != null
                && Instant.now().isBefore(presignedUrlExpiresAt);
    }
}