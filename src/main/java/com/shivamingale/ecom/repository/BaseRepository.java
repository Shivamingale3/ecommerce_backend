package com.shivamingale.ecom.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T> extends JpaRepository<T, String>, JpaSpecificationExecutor<T> {

    Optional<T> findByIdAndDeletedFalse(String id);

    Page<T> findAllByDeletedFalse(Pageable pageable);
}