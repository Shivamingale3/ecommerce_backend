package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.ProductTag;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductTagRepository extends BaseRepository<ProductTag> {

    List<ProductTag> findByProductIdAndDeletedFalse(String productId);

    List<ProductTag> findByTagIdAndDeletedFalse(String tagId);

    boolean existsByProductIdAndTagId(String productId, String tagId);
}