package com.shivamingale.ecom.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shivamingale.ecom.entity.Brand;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrandDTO {

    private String id;
    private String name;
    private String slug;
    private String description;
    private MediaDTO logo;
    private Instant createdAt;

    public static BrandDTO fromEntity(Brand brand) {
        if (brand == null) return null;
        return BrandDTO.builder()
            .id(brand.getId())
            .name(brand.getName())
            .slug(brand.getSlug())
            .description(brand.getDescription())
            .logo(MediaDTO.fromEntity(brand.getLogo()))
            .createdAt(brand.getCreatedAt())
            .build();
    }
}