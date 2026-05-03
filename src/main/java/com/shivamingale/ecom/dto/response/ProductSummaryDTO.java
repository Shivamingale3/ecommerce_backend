package com.shivamingale.ecom.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shivamingale.ecom.entity.Product;
import com.shivamingale.ecom.enums.ProductStatus;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSummaryDTO {

    private String id;
    private String name;
    private String slug;
    private BigDecimal price;
    private String sku;
    private ProductStatus status;
    private boolean featured;
    private boolean visible;
    private BrandSummaryDTO brand;
    private MediaDTO primaryImage;
    private InventorySummaryDTO inventory;

    public static ProductSummaryDTO fromEntity(Product product) {
        if (product == null)
            return null;
        return ProductSummaryDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .price(product.getPrice())
                .sku(product.getSku())
                .status(product.getStatus())
                .featured(product.isFeatured())
                .visible(product.isVisible())
                .brand(BrandSummaryDTO.fromEntity(product.getBrand()))
                .inventory(InventorySummaryDTO.fromEntity(product.getInventory()))
                .build();
    }
}