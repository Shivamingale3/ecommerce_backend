package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.Brand;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends BaseRepository<Brand> {

    Optional<Brand> findBySlug(String slug);

    boolean existsBySlug(String slug);
}