package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.ProductMedia;
import com.shivamingale.ecom.enums.MediaRole;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductMediaRepository extends BaseRepository<ProductMedia> {

    List<ProductMedia> findByProductIdAndDeletedFalseOrderByDisplayOrderAsc(String productId);

    List<ProductMedia> findByProductIdAndRoleAndDeletedFalse(String productId, MediaRole role);

    Optional<ProductMedia> findByProductIdAndMediaIdAndDeletedFalse(String productId, String mediaId);

    boolean existsByProductIdAndMediaId(String productId, String mediaId);

    Optional<ProductMedia> findFirstByProductIdAndDeletedFalseOrderByDisplayOrderAsc(String productId);
}