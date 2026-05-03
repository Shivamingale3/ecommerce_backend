package com.shivamingale.ecom.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shivamingale.ecom.entity.Brand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrandSummaryDTO {

    private String id;
    private String name;
    private String slug;
    private MediaDTO logo;

    public static BrandSummaryDTO fromEntity(Brand brand) {
        if (brand == null)
            return null;
        return BrandSummaryDTO.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .logo(MediaDTO.fromEntity(brand.getLogo()))
                .build();
    }
}