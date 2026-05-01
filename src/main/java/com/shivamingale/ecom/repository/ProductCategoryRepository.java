package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.ProductCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCategoryRepository extends BaseRepository<ProductCategory> {

    List<ProductCategory> findByProductIdAndDeletedFalse(String productId);

    List<ProductCategory> findByCategoryIdAndDeletedFalse(String categoryId);

    Optional<ProductCategory> findByProductIdAndCategoryIdAndDeletedFalse(String productId, String categoryId);

    boolean existsByProductIdAndCategoryId(String productId, String categoryId);

    Optional<ProductCategory> findByProductIdAndPrimaryTrueAndDeletedFalse(String productId);
}