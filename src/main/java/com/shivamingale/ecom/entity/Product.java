package com.shivamingale.ecom.entity;

import com.shivamingale.ecom.enums.ProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = {"brand", "inventory"})
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(precision = 19, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    private BigDecimal cost;

    @Column(unique = true)
    private String sku;

    private String barcode;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "length_cm")
    private Integer lengthCm;

    @Column(name = "width_cm")
    private Integer widthCm;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "meta_title", length = 100)
    private String metaTitle;

    @Column(name = "meta_description", length = 300)
    private String metaDescription;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private boolean visible = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @OneToOne(mappedBy = "product", fetch = FetchType.LAZY)
    private Inventory inventory;
}