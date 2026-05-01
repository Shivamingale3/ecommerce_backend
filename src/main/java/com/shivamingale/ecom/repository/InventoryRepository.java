package com.shivamingale.ecom.repository;

import com.shivamingale.ecom.entity.Inventory;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends BaseRepository<Inventory> {

    boolean existsByProductId(String productId);
}