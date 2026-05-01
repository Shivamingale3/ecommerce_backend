# Phase 6: Inventory Management

**Database Table:** Inventory (id, product_id, stockQuantity, reservedQuantity, lowStockThreshold, deleted, ...)
**Related Tables:** Product (Inventory is OneToOne with Product)

---

## 6.1 List Low Stock Products
**Endpoint:** `GET /api/admin/v1/inventory/low-stock`
**Security:** Required (JWT Bearer token, type="admin")

### Query Parameters
| Param | Type | Default | Rules |
|-------|------|---------|-------|
| limit | Integer | 20 | Min 1, Max 100 |
| offset | Integer | 0 | Min 0 |
| threshold | Integer | null | Override default lowStockThreshold (use this instead of product's threshold) |

### Process
1. Query Inventory where (stockQuantity - reservedQuantity) <= lowStockThreshold
2. If threshold param provided, use that instead
3. Return products with inventory details

### DB Actions
- **Select:** Inventory I, Product P WHERE I.product_id = P.id AND P.deleted = false AND (I.stockQuantity - I.reservedQuantity) <= I.lowStockThreshold

### Response (200)
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "product": {
          "id": "01ARZPROD...",
          "name": "Nike Air Max 90",
          "sku": "NAM90-001"
        },
        "inventory": {
          "stockQuantity": 8,
          "reservedQuantity": 3,
          "availableQuantity": 5,
          "lowStockThreshold": 10,
          "lowStock": true,
          "inStock": true
        }
      }
    ],
    "total": 25,
    "limit": 20,
    "offset": 0
  }
}
```

---

## 6.2 Bulk Stock Update
**Endpoint:** `PUT /api/admin/v1/inventory/bulk`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "items": [
    { "productId": "01ARZPROD1...", "stockQuantity": 100 },
    { "productId": "01ARZPROD2...", "stockQuantity": 50 },
    { "productId": "01ARZPROD3...", "stockQuantity": 200 }
  ]
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| items | Array | Yes | Min 1, Max 100 |
| items[].productId | String | Yes | Must exist |
| items[].stockQuantity | Integer | Yes | >= 0 |

### Process
1. Validate all productIds exist
2. Batch update stockQuantity in Inventory
3. Invalidate product caches
4. Return count

### DB Actions
- **Update:** Inventory SET stockQuantity = :qty WHERE product_id = :productId

### Cache Invalidation
- **Delete:** product:{id} for each product

### Response (200)
```json
{
  "success": true,
  "message": "3 products updated",
  "data": {
    "updatedCount": 3
  }
}
```

---

## 6.3 Stock Adjustment (+/-)
**Endpoint:** `POST /api/admin/v1/inventory/{productId}/adjust`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "adjustment": -5,
  "reason": "Damaged in warehouse"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| adjustment | Integer | Yes | Can be positive or negative |
| reason | String | No | Max 200 chars |

### Process
1. Find Product and Inventory
2. Calculate new stockQuantity = current + adjustment
3. Validate new stockQuantity >= 0
4. Update Inventory
5. Log adjustment (optional: create StockMovement entity in future)
6. Return previous and new values

### DB Actions
- **Select:** Inventory WHERE product_id = :productId
- **Update:** Inventory SET stockQuantity = :newQuantity WHERE id = :id

### Response (200)
```json
{
  "success": true,
  "data": {
    "productId": "01ARZPROD...",
    "previousQuantity": 50,
    "adjustment": -5,
    "newQuantity": 45,
    "availableQuantity": 40,
    "lowStock": false,
    "inStock": true,
    "reason": "Damaged in warehouse"
  },
  "message": "Stock adjusted"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Product not found | Product or Inventory not found |
| BAD_REQUEST | Insufficient stock | Resulting stockQuantity < 0 |

---

## 6.4 Reserve Stock
**Endpoint:** `POST /api/admin/v1/inventory/{productId}/reserve`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "quantity": 2,
  "orderId": "ORD-12345"
}
```

### Process
1. Find Inventory
2. Check availableQuantity >= quantity
3. Update reservedQuantity += quantity
4. (Future: link reservation to orderId)
5. Return updated inventory

### Response (200)
```json
{
  "success": true,
  "data": {
    "productId": "01ARZPROD...",
    "reservedQuantity": 7,
    "availableQuantity": 43,
    "reservationId": "RES-001",
    "orderId": "ORD-12345"
  }
}
```

---

## Notes for Junior Engineers

### Available Quantity Formula
```java
public Integer getAvailableQuantity() {
    return stockQuantity - reservedQuantity;
}
```

### Low Stock Check
```java
public boolean isLowStock() {
    return getAvailableQuantity() <= lowStockThreshold;
}
```

### Stock Cannot Go Negative
```java
if (newStockQuantity < 0) {
    throw new AppException(HttpStatus.BAD_REQUEST, "Insufficient stock", null);
}
```