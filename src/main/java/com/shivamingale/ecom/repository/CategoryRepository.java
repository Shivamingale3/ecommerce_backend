package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends BaseRepository<Category> {

    Optional<Category> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlug(String slug);

    List<Category> findByParentIsNullAndDeletedFalse();

    List<Category> findByParentIdAndDeletedFalse(String parentId);

    Page<Category> findByActiveTrueAndDeletedFalse(Pageable pageable);
}