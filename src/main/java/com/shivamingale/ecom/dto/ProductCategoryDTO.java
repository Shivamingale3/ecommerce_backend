package com.shivamingale.ecom.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shivamingale.ecom.entity.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCategoryDTO {

    private String id;
    private CategoryDTO category;
    private boolean primary;
    private Integer displayOrder;
    private String description;

    public static ProductCategoryDTO fromEntity(ProductCategory productCategory) {
        if (productCategory == null) return null;
        return ProductCategoryDTO.builder()
            .id(productCategory.getId())
            .category(CategoryDTO.fromEntity(productCategory.getCategory()))
            .primary(productCategory.isPrimary())
            .displayOrder(productCategory.getDisplayOrder())
            .description(productCategory.getDescription())
            .build();
    }
}