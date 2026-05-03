package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends BaseRepository<Category> {

    boolean existsById(String id);

    void deleteById(String id);

    Optional<Category> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsByNameIgnoreCase(String name);

    List<Category> findByParentIsNullAndDeletedFalse();

    List<Category> findByParentIdAndDeletedFalse(String parentId);

    List<Category> findByActiveTrueAndDeletedFalseOrderByDepthAsc();

    Page<Category> findByActiveTrueAndDeletedFalseOrderByDepthAsc(Pageable pageable);

    Page<Category> findByActiveTrueAndDeletedFalse(Pageable pageable);

}