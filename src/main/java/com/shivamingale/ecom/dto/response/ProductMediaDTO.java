package com.shivamingale.ecom.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shivamingale.ecom.entity.ProductMedia;
import com.shivamingale.ecom.enums.MediaRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductMediaDTO {

    private String id;
    private MediaDTO media;
    private MediaRole role;
    private Integer displayOrder;
    private String altTextOverride;

    public static ProductMediaDTO fromEntity(ProductMedia productMedia) {
        if (productMedia == null)
            return null;
        return ProductMediaDTO.builder()
                .id(productMedia.getId())
                .media(MediaDTO.fromEntity(productMedia.getMedia()))
                .role(productMedia.getRole())
                .displayOrder(productMedia.getDisplayOrder())
                .altTextOverride(productMedia.getAltTextOverride())
                .build();
    }
}