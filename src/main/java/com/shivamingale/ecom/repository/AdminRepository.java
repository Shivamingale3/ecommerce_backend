package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.Admin;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends BaseRepository<Admin> {

    Optional<Admin> findByEmailAndDeletedFalse(String email);

    boolean existsByEmail(String email);
}
