package com.shivamingale.ecom.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shivamingale.ecom.entity.Product;
import com.shivamingale.ecom.enums.MediaRole;
import com.shivamingale.ecom.enums.ProductStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailDTO {

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
    private List<CategorySummaryDTO> categories;
    private List<TagDTO> tags;
    private List<MediaDTO> media;
    private Map<MediaRole, MediaDTO> mediaByRole;

    public static ProductDetailDTO fromEntity(Product product) {
        if (product == null) return null;
        return ProductDetailDTO.builder()
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

    public static ProductDetailDTO fromEntityWithRelations(Product product,
            List<ProductMediaDTO> productMediaList,
            List<CategorySummaryDTO> categories,
            List<TagDTO> tags) {
        if (product == null) return null;

        ProductDetailDTO dto = fromEntity(product);
        dto.setCategories(categories);
        dto.setTags(tags);

        if (productMediaList != null && !productMediaList.isEmpty()) {
            List<MediaDTO> mediaDTOs = productMediaList.stream()
                .map(ProductMediaDTO::getMedia)
                .collect(Collectors.toList());
            dto.setMedia(mediaDTOs);

            Map<MediaRole, MediaDTO> mediaByRole = productMediaList.stream()
                .filter(pm -> pm.getRole() != null)
                .collect(Collectors.toMap(
                    ProductMediaDTO::getRole,
                    ProductMediaDTO::getMedia,
                    (existing, replacement) -> existing
                ));
            dto.setMediaByRole(mediaByRole);
        }

        return dto;
    }
}