package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.User;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends BaseRepository<User> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    boolean existsByEmail(String email);
}
