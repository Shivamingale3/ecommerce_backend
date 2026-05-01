package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.AdminSignInRequest;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminSignInRequestRepository extends BaseRepository<AdminSignInRequest> {

    Optional<AdminSignInRequest> findByEmailAndDeletedFalse(String email);
}
