package com.shivamingale.invoice.repository;

import com.shivamingale.invoice.entity.Tenant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByOwnerEmail(String ownerEmail);

    boolean existsByOwnerEmail(String ownerEmail);
}
