package com.shivamingale.ecom.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shivamingale.ecom.entity.Inventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventorySummaryDTO {

    private String id;
    private Integer stockQuantity;
    private Integer availableQuantity;
    private boolean lowStock;
    private boolean inStock;

    public static InventorySummaryDTO fromEntity(Inventory inventory) {
        if (inventory == null) return null;
        return InventorySummaryDTO.builder()
            .id(inventory.getId())
            .stockQuantity(inventory.getStockQuantity())
            .availableQuantity(inventory.getAvailableQuantity())
            .lowStock(inventory.isLowStock())
            .inStock(inventory.isInStock())
            .build();
    }
}