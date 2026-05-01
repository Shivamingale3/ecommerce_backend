package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.Tag;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends BaseRepository<Tag> {

    Optional<Tag> findBySlug(String slug);

    boolean existsBySlug(String slug);
}