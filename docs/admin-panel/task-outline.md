# E-Commerce Admin Panel API - Phasewise Task Outline

## Phase 0: Admin Authentication & Profile
### 0.1 Admin Login (OTP)
- Admin requests OTP → Email → Enters OTP → JWT tokens
- **DB:** AdminSignInRequest (email, otp, validTill 5min)
- **Response:** accessToken, refreshToken

### 0.2 Token Refresh
- **DB:** Uses AdminJwtTokenProvider
- **Response:** new accessToken, refreshToken

### 0.3 Admin Profile
- **DB:** Admin (name, email, mobile, enabled)
- **Get:** Return profile
- **Update:** name, mobile

### 0.4 Admin Logout
- No DB action (token is stateless, client discards)

---

## Phase 1: Media Library
### 1.1 Request Upload URL (Admin → Backend → S3)
- **Request:** filename, mimeType, fileSize
- **Backend:** Generate S3 presigned PUT URL (expires 5min)
- **Response:** {uploadUrl, key, mediaId (temp)}
- **Cache:** Store pending upload in Redis {mediaId, key, expiresAt}

### 1.2 Confirm Upload (After S3 upload succeeds)
- **Request:** mediaId (temp), name, type, altText
- **Backend:** Verify upload exists in cache, create Media record
- **Cache:** Delete pending upload entry
- **Response:** Media record with presignedUrl (cached)

### 1.3 List Media (with pagination)
- **Request:** limit, offset, type (filter), active (filter)
- **DB:** Media (name, type, key, url, presignedUrl, presignedUrlExpiresAt, altText, fileSize, mimeType, width, height, active, deleted)
- **Response:** {media: [{id, name, type, url, altText, width, height}], total, limit, offset}

### 1.4 Get Single Media
- **Request:** mediaId
- **Response:** Full Media record

### 1.5 Update Media
- **Request:** name, altText, active
- **DB:** Update Media
- **Response:** Updated Media

### 1.6 Delete Media (Soft)
- **Request:** mediaId
- **DB:** BaseEntity.deleted = true
- **Response:** Success

### 1.7 Get/Refresh Presigned URL
- **Request:** mediaId
- **Backend:** Check hasValidPresignedUrl() → return cached url
- If expired → Generate new presigned URL → Update presignedUrl, presignedUrlExpiresAt → Cache in Redis
- **Response:** {url, expiresAt}

### 1.8 Abandoned Upload Cleanup (Scheduled)
- **Cache:** Redis keys with TTL (auto-expire at 5min)
- On S3 bucket listener: if upload confirmed → delete temp key

---

## Phase 2: Category Management
### 2.1 Create Category
- **Request:** name, parentCategoryId (optional), imageMediaId (optional)
- **DB:** Category (name, slug=auto-generated-from-name, displayOrder=0, active=true, depth=computed, path=computed)
- **Validation:** slug unique, name max 100chars
- **Response:** Category with full data

### 2.2 List Categories (tree structure)
- **Request:** limit, offset, active (filter), parentId (null for root)
- **DB:** Category with children (LAZY) and parent (LAZY)
- **Response:** {categories: [{id, name, slug, active, displayOrder, children[], parent}], total}

### 2.3 Get Single Category
- **Request:** categoryId
- **Response:** Full Category with path computed

### 2.4 Update Category
- **Request:** name, parentCategoryId, imageMediaId, active
- **DB:** Update Category, recalculate path/depth for this and all children
- **Validation:** Prevent circular parent reference (category can't be its own ancestor)
- **Response:** Updated Category

### 2.5 Reorder Categories (Bulk)
- **Request:** [{categoryId, displayOrder}, ...]
- **DB:** Batch update displayOrder
- **Response:** Success

### 2.6 Delete Category (Soft + Cascade Check)
- **Request:** categoryId
- **DB:** Category.deleted = true, ProductCategory.deleted = true for all linked
- **Check:** Warn if has children (soft-delete children too or prevent?)
- **Response:** Success

### 2.7 Category Image Upload
- Uses Phase 1 media flow
- **Request:** categoryId, mediaId (from Phase 1)
- **DB:** Category.image = mediaId
- **Response:** Updated Category

---

## Phase 3: Brand Management
### 3.1 Create Brand
- **Request:** name, description (HTML), logoMediaId (optional)
- **DB:** Brand (name, slug=auto-generated, description, logo)
- **Validation:** slug unique, name max 100chars, description max 2000chars
- **Response:** Brand with logo URL

### 3.2 List Brands (with pagination)
- **Request:** limit, offset, active (filter)
- **DB:** Brand (name, slug, description, logo, active, deleted)
- **Response:** {brands: [{id, name, slug, logo: {url, altText}}, total, limit, offset}

### 3.3 Get Single Brand
- **Request:** brandId
- **Response:** Full Brand

### 3.4 Update Brand
- **Request:** name, description, logoMediaId, active
- **DB:** Update Brand
- **Response:** Updated Brand

### 3.5 Delete Brand (Soft)
- **Request:** brandId
- **DB:** Brand.deleted = true
- **Check:** Unlink from products (set brandId=null) or prevent if products exist?
- **Response:** Success

---

## Phase 4: Tag Management
### 4.1 Create Tag
- **Request:** name
- **DB:** Tag (name, slug=auto-generated)
- **Validation:** slug unique, name max 50chars
- **Response:** Tag

### 4.2 List Tags
- **Request:** limit, offset, search (name filter)
- **DB:** Tag (name, slug, deleted)
- **Response:** {tags: [{id, name, slug}], total, limit, offset}

### 4.3 Update Tag
- **Request:** tagId, name
- **DB:** Update Tag, regenerate slug if name changed
- **Response:** Updated Tag

### 4.4 Delete Tag (Soft)
- **Request:** tagId
- **DB:** Tag.deleted = true
- **Response:** Success

### 4.5 Create Tag Inline (during product creation)
- **Request:** name (during product create flow)
- **DB:** Create if not exists, return Tag
- **Used in:** Phase 5.3

---

## Phase 5: Product Management
### 5.1 Create Product (Step 1 - Basic Info)
- **Request:** name, description, brandId
- **DB:** Product (name, slug=auto-generated, description, status=DRAFT, price=0, featured=false, visible=true, brand)
- **Auto-create:** Inventory (stockQuantity=0, reservedQuantity=0, lowStockThreshold=10)
- **Validation:** name max 200chars, description max 2000chars
- **Response:** {productId, slug}

### 5.2 Update Product Basic Info
- **Request:** productId, name, description, brandId
- **DB:** Update Product fields
- **Response:** Updated Product

### 5.3 Assign Categories and Tags
- **Request:** productId, categoryAssignments: [{categoryId, primary, displayOrder, description}], tagIds: [tagId1, tagId2]
- **DB:** ProductCategory (create new, delete removed), ProductTag (create new, delete removed)
- **Validation:** Only one primary category allowed (auto-unset others), tagIds must exist
- **Response:** {categories: [ProductCategoryDTO], tags: [TagDTO]}

### 5.4 Assign Media to Product
- **Request:** productId, mediaAssignments: [{mediaId, role, displayOrder, altTextOverride}]
- **DB:** ProductMedia (create new, delete removed)
- **Validation:** mediaId must exist, role must be valid MediaRole enum, max role count per product?
- **Response:** {media: [ProductMediaDTO]}

### 5.5 Set Pricing and Inventory
- **Request:** productId, price, cost (optional), sku (optional), barcode (optional)
- **DB:** Product (price, cost, sku, barcode)
- **Validation:** price >= 0, max 4 decimal places, sku unique if provided, barcode max 50chars
- **Response:** Updated Product

### 5.6 Update Inventory (Separate Step)
- **Request:** productId, stockQuantity, lowStockThreshold (optional)
- **DB:** Inventory (stockQuantity, lowStockThreshold)
- **Validation:** stockQuantity >= 0, lowStockThreshold >= 0
- **Response:** {inventory: {stockQuantity, availableQuantity, lowStock, inStock}}

### 5.7 Set Shipping/Dimensions
- **Request:** productId, weightGrams, lengthCm, widthCm, heightCm
- **DB:** Product (weightGrams, lengthCm, widthCm, heightCm)
- **Validation:** all >= 0, weightGrams max 100000g
- **Response:** Updated Product

### 5.8 Set SEO Fields
- **Request:** productId, slug, metaTitle, metaDescription
- **DB:** Product (slug, metaTitle, metaDescription)
- **Validation:** slug unique, slug alphanumeric+hyphens lowercase, metaTitle max 100chars, metaDescription max 300chars
- **Response:** Updated Product

### 5.9 Set Featured/Visibility
- **Request:** productId, featured (bool), visible (bool)
- **DB:** Product (featured, visible)
- **Response:** Updated Product

### 5.10 Change Product Status
- **Request:** productId, status (enum: DRAFT, PENDING_REVIEW, ACTIVE, INACTIVE, ARCHIVED)
- **DB:** Product.status
- **Validation:** Valid status transition
- **Response:** Updated Product

### 5.11 Get Product Detail
- **Request:** productId
- **Response:** Full ProductDetailDTO (product + brand + inventory + categories + tags + media with roles)

### 5.12 List Products (Catalog)
- **Request:** limit, offset, status, categoryId, brandId, featured, visible, search (name/sku)
- **DB:** Product + ProductCategory + Inventory
- **Response:** {products: [ProductSummaryDTO], total, limit, offset}

### 5.13 Delete Product (Soft)
- **Request:** productId
- **DB:** Product.deleted = true, Inventory.deleted = true, ProductCategory.deleted = true, ProductMedia.deleted = true, ProductTag.deleted = true
- **Response:** Success

### 5.14 Bulk Operations
- **Bulk Status Change:** {productIds: [], status}
- **Bulk Delete:** {productIds: []}
- **DB:** Batch update deleted/status
- **Response:** {affectedCount}

---

## Phase 6: Inventory Management (Dedicated Section)
### 6.1 List Products Low in Stock
- **Request:** limit, offset, threshold (optional)
- **DB:** Inventory WHERE availableQuantity <= lowStockThreshold
- **Response:** {products: [{product: ProductSummaryDTO, inventory}], total}

### 6.2 Bulk Stock Update
- **Request:** [{productId, stockQuantity}, ...]
- **DB:** Batch update Inventory
- **Response:** {updatedCount}

### 6.3 Stock Adjustment (+/-)
- **Request:** productId, adjustment (+/- integer), reason
- **DB:** Inventory.stockQuantity += adjustment
- **Response:** {inventory: {previousQuantity, newQuantity, availableQuantity}}

### 6.4 Reserved Stock Management
- **Admin manually reserves stock for orders (later connected to Order module)**
- **Request:** productId, reserveQuantity
- **DB:** Inventory.reservedQuantity += reserveQuantity
- **Response:** {availableQuantity, reservedQuantity}

---

## Phase 7: Media Library (Product Assignment View)
### 7.1 Get Unassigned Media
- **Request:** limit, offset, type
- **DB:** Media WHERE NOT EXISTS (ProductMedia WHERE mediaId = Media.id AND deleted=false)
- **Response:** {media: [MediaDTO], total}

### 7.2 Get Media by Role Type
- **Request:** productId, role
- **DB:** ProductMedia WHERE productId AND role AND deleted=false
- **Response:** {media: [ProductMediaDTO]}

---

## Validation Summary Table

| Entity | Field | DB Type | Java Type | Constraints | Max Length |
|--------|-------|---------|-----------|--------------|------------|
| Product | name | VARCHAR | String | required | 200 |
| Product | slug | VARCHAR | String | required, unique, lowercase-alphanumeric-hyphens | 200 |
| Product | description | TEXT | String | optional | 2000 |
| Product | status | ENUM | ProductStatus | required, default DRAFT | - |
| Product | price | DECIMAL(19,4) | BigDecimal | required, >= 0 | - |
| Product | cost | DECIMAL(19,4) | BigDecimal | optional | - |
| Product | sku | VARCHAR | String | optional, unique | 50 |
| Product | barcode | VARCHAR | String | optional | 50 |
| Product | weightGrams | INT | Integer | optional, >= 0 | - |
| Product | lengthCm/widthCm/heightCm | INT | Integer | optional, >= 0 | - |
| Product | metaTitle | VARCHAR | String | optional | 100 |
| Product | metaDescription | VARCHAR | String | optional | 300 |
| Product | featured/visible | BOOLEAN | boolean | default false/true | - |
| Category | name | VARCHAR | String | required | 100 |
| Category | slug | VARCHAR | String | required, unique, lowercase | 100 |
| Category | description | VARCHAR | String | optional | 500 |
| Category | displayOrder | INT | Integer | default 0 | - |
| Category | active | BOOLEAN | boolean | default true | - |
| Category | path | VARCHAR | String | computed | 500 |
| Category | depth | INT | Integer | computed, default 0 | - |
| Brand | name | VARCHAR | String | required | 100 |
| Brand | slug | VARCHAR | String | required, unique | 100 |
| Brand | description | TEXT | String | optional (HTML) | 2000 |
| Tag | name | VARCHAR | String | required | 50 |
| Tag | slug | VARCHAR | String | required, unique | 50 |
| Media | name | VARCHAR | String | required | 100 |
| Media | type | ENUM | MediaType | required | - |
| Media | key | VARCHAR | String | required | 100 |
| Media | altText | VARCHAR | String | optional | 300 |
| Media | fileSize | BIGINT | Long | optional | - |
| Media | mimeType | VARCHAR | String | optional | 100 |
| Media | width/height | INT | Integer | optional | - |
| Media | active | BOOLEAN | boolean | default true | - |
| Inventory | stockQuantity | INT | Integer | required, default 0, >= 0 | - |
| Inventory | reservedQuantity | INT | Integer | default 0, >= 0 | - |
| Inventory | lowStockThreshold | INT | Integer | default 10, >= 0 | - |
| ProductMedia | role | ENUM | MediaRole | required, default GALLERY | - |
| ProductMedia | displayOrder | INT | Integer | default 0 | - |
| ProductMedia | altTextOverride | VARCHAR | String | optional | 300 |
| ProductCategory | primary | BOOLEAN | boolean | default false | - |
| ProductCategory | displayOrder | INT | Integer | default 0 | - |
| ProductCategory | description | VARCHAR | String | optional | 500 |
| Admin | name | VARCHAR | String | required | 100 |
| Admin | email | VARCHAR | String | required, unique | 100 |
| Admin | mobile | VARCHAR | String | required | 20 |

---

## File Upload Flow (Media)

### Step 1: Request Presigned URL
```
POST /api/admin/v1/media/upload-url
Request: { filename: "hero.jpg", mimeType: "image/jpeg", fileSize: 2048000 }
Response: { uploadUrl: "https://s3.../presigned-put-url", key: "products/abc123/hero.jpg", mediaId: "temp_123" }
Cache: Redis key "pending_upload:{mediaId}" with TTL 5min
```

### Step 2: Upload to S3 (Direct)
```
PUT {uploadUrl}
Body: [binary file data]
Headers: Content-Type: image/jpeg
Response: 200 OK (S3 response)
```

### Step 3: Confirm Upload
```
POST /api/admin/v1/media/confirm-upload
Request: { mediaId: "temp_123", name: "Product Hero Image", type: "IMAGE", altText: "Product hero" }
Backend: Check Redis → Create Media record → Delete Redis key
Response: { id: "01ARZ3...", name: "Product Hero Image", type: "IMAGE", url: "...", presignedUrl: "..." }
```

### Step 4: Get Display URL (when needed)
```
GET /api/admin/v1/media/{id}/url
Backend: Media.hasValidPresignedUrl() → return cached url
If expired: Generate new presigned GET URL → Update DB → Update Cache
Response: { url: "...", expiresAt: "..." }
```

---

## API Response Format (Standard)
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed"
}
```

## Paginated Response
```json
{
  "success": true,
  "data": {
    "items": [...],
    "total": 150,
    "limit": 20,
    "offset": 0
  }
}
```

## Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Price must be greater than 0",
    "field": "price"
  }
}
```

---

## Endpoint Naming Convention
```
/api/admin/v1/{resource}
/api/admin/v1/{resource}/{id}
/api/admin/v1/{resource}/{id}/{sub-resource}
/api/admin/v1/{resource}/bulk
```

## Pagination
```
Query params: ?limit=20&offset=0
Default limit: 20
Max limit: 100
```

---

## Cache Strategy
| Data | Cache Key Pattern | TTL | Invalidation |
|------|------------------|-----|----------------|
| Product listing | `products:list:{hash(params)}` | 5min | On product create/update/delete |
| Product detail | `product:{id}` | 10min | On product update |
| Category tree | `categories:tree` | 30min | On category create/update/delete |
| Brand list | `brands:list` | 30min | On brand create/update/delete |
| Pending upload | `pending_upload:{mediaId}` | 5min | Auto-expire / on confirm |
| Presigned URL | `media:url:{id}` | 55min | Auto-expire / on refresh |

---

## Soft Delete Behavior
All entities extend BaseEntity with `deleted=false` by default.
- List queries: Automatically filter `deleted=false`
- Detail queries: Return 404 for deleted entities
- Cascade soft delete: When deleting parent, child entities also soft-delete via service layer

## Unique Constraints
| Entity | Constraint Columns |
|--------|-------------------|
| Product | slug, sku (if not null) |
| Category | slug |
| Brand | slug |
| Tag | slug |
| ProductCategory | product_id + category_id |
| ProductMedia | product_id + media_id |
| ProductTag | product_id + tag_id |
| Inventory | product_id (unique) |
| Admin | email |
| User | email |
