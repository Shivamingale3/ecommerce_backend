# Phase 5: Product Management

**Database Tables:**
- Product (id, name, slug, description, status, price, cost, sku, barcode, weightGrams, lengthCm, widthCm, heightCm, metaTitle, metaDescription, featured, visible, brand_id, inventory_id, deleted, ...)
- Inventory (id, product_id, stockQuantity, reservedQuantity, lowStockThreshold, deleted, ...)
- ProductCategory (id, product_id, category_id, primary, displayOrder, description, deleted, ...)
- ProductMedia (id, product_id, media_id, role, displayOrder, altTextOverride, deleted, ...)
- ProductTag (id, product_id, tag_id, deleted, ...)
- Category, Brand, Tag, Media (referenced entities)

**Cache Patterns:**
- `products:list:{hash(params)}` - TTL 5min - paginated product list
- `product:{id}` - TTL 10min - single product detail

**Related Phases:** 1 (Media), 2 (Category), 3 (Brand), 4 (Tag), 6 (Inventory)

---

## 5.1 Create Product (Step 1 - Basic Info)
**Endpoint:** `POST /api/admin/v1/products`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "name": "Nike Air Max 90",
  "description": "The Nike Air Max 90 introduces a fresh take on the iconic design.",
  "brandId": "01ARZBRAND..."
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| name | String | Yes | Max 200 chars, not blank |
| description | String | No | Max 2000 chars |
| brandId | String | No | Must exist if provided |

### Process
1. Validate name
2. Generate slug from name (check uniqueness)
3. Create Product with defaults:
   - status = DRAFT
   - price = 0
   - featured = false
   - visible = true
4. Auto-create Inventory record:
   - product → Product (OneToOne, mappedBy = "product")
   - stockQuantity = 0
   - reservedQuantity = 0
   - lowStockThreshold = 10
5. Return product with temporary ID for next steps

### Inventory Auto-Creation
```java
@Transactional
public Product createProduct(CreateProductRequest request) {
    Product product = Product.builder()
        .name(request.getName())
        .description(request.getDescription())
        .slug(generateSlug(request.getName()))
        .status(ProductStatus.DRAFT)
        .price(BigDecimal.ZERO)
        .featured(false)
        .visible(true)
        .build();

    if (request.getBrandId() != null) {
        Brand brand = brandRepository.findByIdAndDeletedFalse(request.getBrandId())
            .orElseThrow(() -> new NotFoundException("Brand not found"));
        product.setBrand(brand);
    }

    product = productRepository.save(product);

    // Auto-create inventory
    Inventory inventory = Inventory.builder()
        .product(product)
        .stockQuantity(0)
        .reservedQuantity(0)
        .lowStockThreshold(10)
        .build();
    inventoryRepository.save(inventory);

    product.setInventory(inventory);
    return product;
}
```

### DB Actions
- **Insert:** Product (name, slug, description, status, price, featured, visible, brand_id)
- **Insert:** Inventory (product_id, stockQuantity, reservedQuantity, lowStockThreshold)
- **Select:** Brand (if brandId provided)

### Cache Invalidation
- **Delete:** products:list:*

### Response (201)
```json
{
  "success": true,
  "data": {
    "id": "01ARZPROD...",
    "name": "Nike Air Max 90",
    "slug": "nike-air-max-90",
    "description": "The Nike Air Max 90 introduces a fresh take on the iconic design.",
    "status": "DRAFT",
    "price": 0,
    "featured": false,
    "visible": true,
    "brand": {
      "id": "01ARZBRAND...",
      "name": "Nike",
      "slug": "nike"
    },
    "inventory": {
      "id": "01ARZINV...",
      "stockQuantity": 0,
      "availableQuantity": 0,
      "lowStock": false,
      "inStock": false
    },
    "createdAt": "2026-05-01T10:00:00Z"
  },
  "message": "Product created successfully"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| BAD_REQUEST | Name is required | name is blank |
| BAD_REQUEST | Name exceeds 200 characters | name.length() > 200 |
| BAD_REQUEST | Description exceeds 2000 characters | description.length() > 2000 |
| BAD_REQUEST | Slug already exists | slug not unique |
| NOT_FOUND | Brand not found | brandId not found |

---

## 5.2 List Products
**Endpoint:** `GET /api/admin/v1/products`
**Security:** Required (JWT Bearer token, type="admin")

### Query Parameters
| Param | Type | Default | Rules |
|-------|------|---------|-------|
| limit | Integer | 20 | Min 1, Max 100 |
| offset | Integer | 0 | Min 0 |
| status | String | null | Filter by ProductStatus |
| categoryId | String | null | Filter by category (includes subcategories) |
| brandId | String | null | Filter by brand |
| featured | Boolean | null | Filter by featured flag |
| visible | Boolean | null | Filter by visible flag |
| search | String | null | Search in name or sku (LIKE %search%) |
| sort | String | "createdAt,desc" | Sort field and direction |

### Process
1. Build query with BaseRepository
2. Apply status filter
3. Apply category filter (use category.path LIKE for subcategories)
4. Apply brand filter
5. Apply featured/visible filters
6. Apply search filter
7. Sort and paginate
8. Return list with inventory count indicator

### Cache Strategy
```java
// Generate cache key from all params
String cacheKey = "products:list:" + MD5(JSON.of(params));
// Invalidate on any product create/update/delete
```

### DB Actions
- **Select:** Product WHERE deleted = false
- **Join:** Category, Brand, Inventory as needed
- **Count:** Total matching products

### Response (200)
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "01ARZPROD...",
        "name": "Nike Air Max 90",
        "slug": "nike-air-max-90",
        "price": 12999.00,
        "sku": "NAM90-001",
        "status": "ACTIVE",
        "featured": true,
        "visible": true,
        "brand": {
          "id": "01ARZBRAND...",
          "name": "Nike",
          "slug": "nike"
        },
        "primaryImage": {
          "id": "01ARZMEDIA...",
          "url": "https://cdn.example.com/products/nike-air-max-90.jpg"
        },
        "inventory": {
          "stockQuantity": 50,
          "availableQuantity": 45,
          "lowStock": false,
          "inStock": true
        }
      }
    ],
    "total": 150,
    "limit": 20,
    "offset": 0
  }
}
```

---

## 5.3 Get Product Detail
**Endpoint:** `GET /api/admin/v1/products/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Process
1. Find Product by ID (include brand, inventory)
2. Fetch categories via ProductCategoryRepository
3. Fetch tags via ProductTagRepository
4. Fetch media via ProductMediaRepository
5. Assemble full ProductDetailDTO
6. Cache result

### DB Actions
- **Select:** Product WHERE id = :id AND deleted = false
- **Select:** ProductCategory WHERE product_id = :id AND deleted = false
- **Select:** ProductMedia WHERE product_id = :id AND deleted = false
- **Select:** ProductTag WHERE product_id = :id AND deleted = false
- **Select:** Category, Media, Tag (related entities)

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZPROD...",
    "name": "Nike Air Max 90",
    "slug": "nike-air-max-90",
    "description": "The Nike Air Max 90 introduces a fresh take on the iconic design.",
    "status": "ACTIVE",
    "price": 12999.00,
    "cost": 6500.00,
    "sku": "NAM90-001",
    "barcode": "194500123456",
    "weightGrams": 320,
    "lengthCm": 30,
    "widthCm": 18,
    "heightCm": 12,
    "metaTitle": "Nike Air Max 90 - Best Running Shoes",
    "metaDescription": "Shop the Nike Air Max 90 at best price...",
    "featured": true,
    "visible": true,
    "brand": {
      "id": "01ARZBRAND...",
      "name": "Nike",
      "slug": "nike"
    },
    "inventory": {
      "id": "01ARZINV...",
      "stockQuantity": 50,
      "reservedQuantity": 5,
      "availableQuantity": 45,
      "lowStockThreshold": 10,
      "lowStock": false,
      "inStock": true
    },
    "categories": [
      {
        "id": "01ARZCAT...",
        "name": "Running Shoes",
        "slug": "running-shoes",
        "primary": true,
        "displayOrder": 0
      }
    ],
    "tags": [
      { "id": "01ARZTAG1...", "name": "Best Seller", "slug": "best-seller" },
      { "id": "01ARZTAG2...", "name": "New Arrival", "slug": "new-arrival" }
    ],
    "media": [
      {
        "id": "01ARZPM1...",
        "media": {
          "id": "01ARZMEDIA...",
          "name": "Hero Image",
          "url": "https://cdn.example.com/...",
          "altText": "Nike Air Max 90"
        },
        "role": "COVER",
        "displayOrder": 0
      }
    ],
    "mediaByRole": {
      "COVER": { "id": "01ARZMEDIA...", "url": "..." },
      "GRID": { "id": "01ARZMEDIA2...", "url": "..." }
    },
    "createdAt": "2026-05-01T10:00:00Z",
    "updatedAt": "2026-05-01T12:00:00Z"
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Product not found | Product not found or deleted |

---

## 5.4 Update Product Basic Info
**Endpoint:** `PUT /api/admin/v1/products/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "name": "Nike Air Max 90 (Updated)",
  "description": "Updated description",
  "brandId": "01ARZBRAND2..."
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| name | String | No | Max 200 chars |
| description | String | No | Max 2000 chars |
| brandId | String | No | Null to remove, or valid brand |

### Process
1. Find Product
2. Update only provided fields
3. If brandId changed → validate new brand exists
4. If name changed → regenerate slug (check uniqueness)
5. Invalidate caches
6. Return updated Product

### DB Actions
- **Update:** Product (name, slug, description, brand_id)
- **Select:** Brand (if brandId provided)

### Cache Invalidation
- **Delete:** product:{id}
- **Delete:** products:list:*

### Response (200)
```json
{
  "success": true,
  "data": { /* full updated Product object */ },
  "message": "Product basic info updated"
}
```

---

## 5.5 Assign Categories and Tags
**Endpoint:** `PUT /api/admin/v1/products/{id}/relations`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "categories": [
    {
      "categoryId": "01ARZCAT1...",
      "primary": true,
      "displayOrder": 0,
      "description": "Primary category for this product"
    },
    {
      "categoryId": "01ARZCAT2...",
      "primary": false,
      "displayOrder": 1
    }
  ],
  "tagIds": ["01ARZTAG1...", "01ARZTAG2...", "01ARZTAG3..."]
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| categories | Array | No | If provided, each categoryId must exist |
| categories[].categoryId | String | Yes | Must exist |
| categories[].primary | Boolean | No | Default false |
| categories[].displayOrder | Integer | No | Default 0 |
| categories[].description | String | No | Max 500 chars |
| tagIds | Array | No | Each must exist |
| tagIds[] | String | Yes | Must exist |

### Process
1. Find Product
2. If categories provided:
   a. Validate all categoryIds exist
   b. If multiple primary=true → keep only first, unset others
   c. Delete existing ProductCategory not in new list
   d. Create new ProductCategory records
3. If tagIds provided:
   a. Validate all tagIds exist
   b. Delete existing ProductTag not in new list
   c. Create new ProductTag records
4. Invalidate caches
5. Return updated categories and tags

### DB Actions
- **Select:** Product WHERE id = :id
- **Select:** Category (for each categoryId)
- **Select:** Tag (for each tagId)
- **Delete:** ProductCategory WHERE product_id = :id AND category_id NOT IN (:newCategoryIds)
- **Insert:** ProductCategory (for new categories)
- **Delete:** ProductTag WHERE product_id = :id AND tag_id NOT IN (:newTagIds)
- **Insert:** ProductTag (for new tags)

### Cache Invalidation
- **Delete:** product:{id}

### Response (200)
```json
{
  "success": true,
  "data": {
    "categories": [
      {
        "id": "01ARZPC1...",
        "category": { "id": "...", "name": "Running Shoes", "slug": "running-shoes" },
        "primary": true,
        "displayOrder": 0,
        "description": "Primary category"
      },
      {
        "id": "01ARZPC2...",
        "category": { "id": "...", "name": "Sports", "slug": "sports" },
        "primary": false,
        "displayOrder": 1
      }
    ],
    "tags": [
      { "id": "01ARZTAG1...", "name": "Best Seller", "slug": "best-seller" },
      { "id": "01ARZTAG2...", "name": "New Arrival", "slug": "new-arrival" },
      { "id": "01ARZTAG3...", "name": "Flash Sale", "slug": "flash-sale" }
    ]
  },
  "message": "Product relations updated"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Product not found | Product not found |
| NOT_FOUND | Category not found | One or more categoryIds not found |
| NOT_FOUND | Tag not found | One or more tagIds not found |
| BAD_REQUEST | Only one primary category allowed | Multiple primary=true |

---

## 5.6 Assign Media to Product
**Endpoint:** `PUT /api/admin/v1/products/{id}/media`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "media": [
    {
      "mediaId": "01ARZMEDIA1...",
      "role": "COVER",
      "displayOrder": 0,
      "altTextOverride": "Nike Air Max 90 Front View"
    },
    {
      "mediaId": "01ARZMEDIA2...",
      "role": "GRID",
      "displayOrder": 1
    },
    {
      "mediaId": "01ARZMEDIA3...",
      "role": "GALLERY",
      "displayOrder": 2
    }
  ]
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| media | Array | Yes | Min 1 item |
| media[].mediaId | String | Yes | Must exist |
| media[].role | Enum | Yes | Must be: MAIN, COVER, GRID, THUMBNAIL, GALLERY, REVIEW |
| media[].displayOrder | Integer | No | Default 0 |
| media[].altTextOverride | String | No | Max 300 chars |

### Process
1. Find Product
2. Validate all mediaIds exist
3. Delete existing ProductMedia not in new list
4. Create new ProductMedia records
5. Invalidate caches
6. Return updated media

### DB Actions
- **Select:** Media (for each mediaId)
- **Delete:** ProductMedia WHERE product_id = :id AND media_id NOT IN (:newMediaIds)
- **Insert:** ProductMedia (for new media)

### Cache Invalidation
- **Delete:** product:{id}

### Response (200)
```json
{
  "success": true,
  "data": {
    "media": [
      {
        "id": "01ARZPM1...",
        "media": {
          "id": "01ARZMEDIA1...",
          "name": "Hero Image",
          "url": "https://cdn.example.com/...",
          "type": "IMAGE",
          "width": 1920,
          "height": 1080
        },
        "role": "COVER",
        "displayOrder": 0,
        "altTextOverride": "Nike Air Max 90 Front View"
      }
    ],
    "mediaByRole": {
      "COVER": { "id": "01ARZMEDIA1...", "url": "..." },
      "GRID": { "id": "01ARZMEDIA2...", "url": "..." }
    }
  },
  "message": "Product media updated"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Product not found | Product not found |
| NOT_FOUND | Media not found | One or more mediaIds not found |
| BAD_REQUEST | At least one media required | media array empty |
| BAD_REQUEST | Invalid role | role not in MediaRole enum |

---

## 5.7 Set Pricing
**Endpoint:** `PUT /api/admin/v1/products/{id}/pricing`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "price": 12999.00,
  "cost": 6500.00,
  "sku": "NAM90-001",
  "barcode": "194500123456"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| price | BigDecimal | Yes | >= 0, max 4 decimal places |
| cost | BigDecimal | No | >= 0, max 4 decimal places |
| sku | String | No | Max 50 chars, unique if provided |
| barcode | String | No | Max 50 chars |

### Process
1. Find Product
2. Validate price >= 0
3. If sku provided and different from current → check uniqueness
4. Update pricing fields
5. Invalidate caches
6. Return updated Product

### DB Actions
- **Update:** Product (price, cost, sku, barcode)
- **Check:** sku uniqueness (if changing)

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZPROD...",
    "price": 12999.00,
    "cost": 6500.00,
    "sku": "NAM90-001",
    "barcode": "194500123456"
  },
  "message": "Pricing updated"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| BAD_REQUEST | Price must be >= 0 | price < 0 |
| BAD_REQUEST | Cost must be >= 0 | cost < 0 |
| BAD_REQUEST | SKU already exists | sku not unique |

---

## 5.8 Update Inventory
**Endpoint:** `PUT /api/admin/v1/products/{id}/inventory`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "stockQuantity": 100,
  "lowStockThreshold": 15
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| stockQuantity | Integer | Yes | >= 0 |
| lowStockThreshold | Integer | No | >= 0, default 10 |

### Process
1. Find Product
2. Find Inventory by productId
3. Update inventory fields
4. Return updated Inventory

### DB Actions
- **Update:** Inventory (stockQuantity, lowStockThreshold) WHERE product_id = :id

### Cache Invalidation
- **Delete:** product:{id}

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZINV...",
    "stockQuantity": 100,
    "reservedQuantity": 5,
    "availableQuantity": 95,
    "lowStockThreshold": 15,
    "lowStock": false,
    "inStock": true
  },
  "message": "Inventory updated"
}
```

---

## 5.9 Set Shipping/Dimensions
**Endpoint:** `PUT /api/admin/v1/products/{id}/shipping`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "weightGrams": 320,
  "lengthCm": 30,
  "widthCm": 18,
  "heightCm": 12
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| weightGrams | Integer | No | >= 0, max 100000 |
| lengthCm | Integer | No | >= 0 |
| widthCm | Integer | No | >= 0 |
| heightCm | Integer | No | >= 0 |

### DB Actions
- **Update:** Product (weightGrams, lengthCm, widthCm, heightCm)

### Response (200)
```json
{
  "success": true,
  "data": {
    "weightGrams": 320,
    "lengthCm": 30,
    "widthCm": 18,
    "heightCm": 12
  },
  "message": "Shipping dimensions updated"
}
```

---

## 5.10 Set SEO Fields
**Endpoint:** `PUT /api/admin/v1/products/{id}/seo`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "slug": "nike-air-max-90-white",
  "metaTitle": "Nike Air Max 90 - Best Running Shoes",
  "metaDescription": "Shop the Nike Air Max 90 at best price. Free shipping on orders over $50."
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| slug | String | No | lowercase, alphanumeric with hyphens, unique |
| metaTitle | String | No | Max 100 chars |
| metaDescription | String | No | Max 300 chars |

### Process
1. Find Product
2. If slug changed → validate format, check uniqueness
3. Update SEO fields
4. Invalidate caches
5. Return updated Product

### DB Actions
- **Update:** Product (slug, metaTitle, metaDescription)
- **Check:** slug uniqueness (if changing)

### Response (200)
```json
{
  "success": true,
  "data": {
    "slug": "nike-air-max-90-white",
    "metaTitle": "Nike Air Max 90 - Best Running Shoes",
    "metaDescription": "Shop the Nike Air Max 90 at best price. Free shipping..."
  },
  "message": "SEO fields updated"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| BAD_REQUEST | Invalid slug format | slug doesn't match pattern |
| BAD_REQUEST | Slug already exists | slug not unique |
| BAD_REQUEST | Meta title exceeds 100 characters | metaTitle.length() > 100 |
| BAD_REQUEST | Meta description exceeds 300 characters | metaDescription.length() > 300 |

---

## 5.11 Set Featured/Visibility
**Endpoint:** `PUT /api/admin/v1/products/{id}/visibility`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "featured": true,
  "visible": false
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| featured | Boolean | No | true or false |
| visible | Boolean | No | true or false |

### DB Actions
- **Update:** Product (featured, visible)

### Cache Invalidation
- **Delete:** product:{id}
- **Delete:** products:list:*

### Response (200)
```json
{
  "success": true,
  "data": {
    "featured": true,
    "visible": false
  },
  "message": "Visibility updated"
}
```

---

## 5.12 Change Product Status
**Endpoint:** `PUT /api/admin/v1/products/{id}/status`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "status": "ACTIVE"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| status | Enum | Yes | Must be: DRAFT, PENDING_REVIEW, ACTIVE, INACTIVE, ARCHIVED |

### Process
1. Find Product
2. Update status
3. Invalidate caches
4. Return updated Product

### DB Actions
- **Update:** Product (status)

### Cache Invalidation
- **Delete:** product:{id}
- **Delete:** products:list:*

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZPROD...",
    "status": "ACTIVE"
  },
  "message": "Product status updated to ACTIVE"
}
```

---

## 5.13 Delete Product (Soft)
**Endpoint:** `DELETE /api/admin/v1/products/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Process
1. Find Product
2. Soft delete Product (deleted = true)
3. Soft delete Inventory (deleted = true)
4. Soft delete all ProductCategory (deleted = true)
5. Soft delete all ProductMedia (deleted = true)
6. Soft delete all ProductTag (deleted = true)
7. Invalidate caches
8. Return success

### DB Actions
- **Update:** Product SET deleted = true WHERE id = :id
- **Update:** Inventory SET deleted = true WHERE product_id = :id
- **Update:** ProductCategory SET deleted = true WHERE product_id = :id
- **Update:** ProductMedia SET deleted = true WHERE product_id = :id
- **Update:** ProductTag SET deleted = true WHERE product_id = :id

### Cache Invalidation
- **Delete:** product:{id}
- **Delete:** products:list:*

### Response (200)
```json
{
  "success": true,
  "message": "Product deleted successfully",
  "data": {
    "deletedRelations": {
      "inventory": 1,
      "categories": 2,
      "media": 5,
      "tags": 3
    }
  }
}
```

---

## 5.14 Bulk Status Change
**Endpoint:** `PUT /api/admin/v1/products/bulk/status`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "productIds": ["01ARZPROD1...", "01ARZPROD2...", "01ARZPROD3..."],
  "status": "ACTIVE"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| productIds | Array | Yes | Min 1, Max 100 |
| productIds[] | String | Yes | Each must exist |
| status | Enum | Yes | Valid ProductStatus |

### Process
1. Validate all productIds exist
2. Batch update status
3. Invalidate all affected product caches
4. Return count

### DB Actions
- **Update:** Product SET status = :status WHERE id IN (:productIds) AND deleted = false

### Cache Invalidation
- **Delete:** product:{id} for each product
- **Delete:** products:list:*

### Response (200)
```json
{
  "success": true,
  "message": "3 products updated to ACTIVE",
  "data": {
    "updatedCount": 3
  }
}
```

---

## 5.15 Bulk Delete
**Endpoint:** `DELETE /api/admin/v1/products/bulk`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "productIds": ["01ARZPROD1...", "01ARZPROD2..."]
}
```

### Process
1. Soft delete all products and their relations
2. Invalidate caches
3. Return count

### DB Actions
- **Update:** Product SET deleted = true WHERE id IN (:productIds)
- **Update:** Inventory SET deleted = true WHERE product_id IN (:productIds)
- **Update:** ProductCategory SET deleted = true WHERE product_id IN (:productIds)
- **Update:** ProductMedia SET deleted = true WHERE product_id IN (:productIds)
- **Update:** ProductTag SET deleted = true WHERE product_id IN (:productIds)

### Response (200)
```json
{
  "success": true,
  "message": "2 products deleted",
  "data": {
    "deletedCount": 2
  }
}
```

---

## Notes for Junior Engineers

### Product Status Flow
```
DRAFT → PENDING_REVIEW → ACTIVE → INACTIVE → ARCHIVED
  ↑         ↓
  ←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←
```
No enforced rules - admin can change to any status. Status affects visibility on frontend.

### Slug Generation
```java
public String generateSlug(String name) {
    return name.toLowerCase()
        .replaceAll("[^a-z0-9\\s-]", "")
        .replaceAll("\\s+", "-")
        .replaceAll("-+", "-")
        .trim();
}
```

### Price Format
- Frontend sends: `"12999.00"` or `12999`
- Backend stores as BigDecimal with 4 decimal precision
- Always use BigDecimal for monetary values

### Primary Category Logic
```java
private void ensureSinglePrimary(List<ProductCategory> categories) {
    long primaryCount = categories.stream()
        .filter(ProductCategory::isPrimary)
        .count();
    if (primaryCount > 1) {
        // Keep first, unset others
        boolean foundFirst = false;
        for (ProductCategory pc : categories) {
            if (pc.isPrimary() && !foundFirst) {
                foundFirst = true;
            } else if (pc.isPrimary()) {
                pc.setPrimary(false);
            }
        }
    }
}
```

### Media Role Default
If not specified, role defaults to GALLERY. Admin should explicitly set roles.

### Category Path Search for Subcategories
```java
// To find all products in a category and its subcategories:
@Query("SELECT DISTINCT pc.product FROM ProductCategory pc " +
       "JOIN pc.category c " +
       "WHERE c.path LIKE :pathPrefix%")
List<Product> findByCategoryAndSubcategories(@Param("pathPrefix") String path);
```

### Multi-Step Form Flow
```
Step 1: Basic Info → POST /products (creates product, returns id)
Step 2: Pricing → PUT /products/{id}/pricing
Step 3: Shipping → PUT /products/{id}/shipping
Step 4: SEO → PUT /products/{id}/seo
Step 5: Relations → PUT /products/{id}/relations (categories, tags)
Step 6: Media → PUT /products/{id}/media
Step 7: Inventory → PUT /products/{id}/inventory
Step 8: Visibility → PUT /products/{id}/visibility
Step 9: Status → PUT /products/{id}/status (publish)
```

Frontend saves product id in localStorage between steps and passes it in each request.
