package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.Product;
import com.shivamingale.ecom.enums.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends BaseRepository<Product> {

    Optional<Product> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    Page<Product> findByStatusAndDeletedFalse(ProductStatus status, Pageable pageable);

    Page<Product> findByVisibleTrueAndStatusAndDeletedFalse(ProductStatus status, Pageable pageable);

    List<Product> findByFeaturedTrueAndVisibleTrueAndStatusAndDeletedFalse(ProductStatus status);
}