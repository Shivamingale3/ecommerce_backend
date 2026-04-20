package com.shivamingale.ecom.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.shivamingale.ecom.entity.AdminSignInRequest;

@Repository
public interface AdminSignInRequestRepository extends JpaRepository<AdminSignInRequest, String> {
    Optional<AdminSignInRequest> findByEmail(String email);
}
