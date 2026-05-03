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
public class ProductDTO {

    private String id;
    private String name;
    private String slug;
    private String description;
    private ProductStatus status;
    private BigDecimal price;
    private BigDecimal cost;
    private String sku;
    private String barcode;
    private Integer weightGrams;
    private Integer lengthCm;
    private Integer widthCm;
    private Integer heightCm;
    private String metaTitle;
    private String metaDescription;
    private boolean featured;
    private boolean visible;
    private BrandDTO brand;
    private InventorySummaryDTO inventory;

    public static ProductDTO fromEntity(Product product) {
        if (product == null)
            return null;
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .status(product.getStatus())
                .price(product.getPrice())
                .cost(product.getCost())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .weightGrams(product.getWeightGrams())
                .lengthCm(product.getLengthCm())
                .widthCm(product.getWidthCm())
                .heightCm(product.getHeightCm())
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())
                .featured(product.isFeatured())
                .visible(product.isVisible())
                .brand(BrandDTO.fromEntity(product.getBrand()))
                .inventory(InventorySummaryDTO.fromEntity(product.getInventory()))
                .build();
    }
}