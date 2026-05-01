# Phase 3: Brand Management

**Database Table:** Brand (id, name, slug, description, logo_id, deleted, createdAt, updatedAt, version)
**Related Tables:** Media (Brand.logo = Media.id), Product (Brand referenced via Product.brand)

---

## 3.1 Create Brand
**Endpoint:** `POST /api/admin/v1/brands`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "name": "Nike",
  "description": "<p>Nike is a world-renowned sportswear brand known for innovation and quality.</p>",
  "logoMediaId": "01ARZ9NDE..."
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| name | String | Yes | Max 100 chars, not blank |
| description | String | No | Max 2000 chars, HTML allowed (needs sanitization) |
| logoMediaId | String | No | Must exist and be IMAGE type if provided |

### Process
1. Validate name (not blank, max 100 chars)
2. Generate slug from name (lowercase, alphanumeric + hyphens)
3. Check slug uniqueness
4. If logoMediaId provided:
   - Verify Media exists, type == IMAGE
5. Create Brand
6. Return Brand with logo URL

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

### DB Actions
- **Insert:** Brand (name, slug, description, logo_id)
- **Check:** slug uniqueness
- **Check:** logoMedia exists if provided

### Cache Invalidation
- Invalidate `brands:list` cache

### Response (201)
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "name": "Nike",
    "slug": "nike",
    "description": "<p>Nike is a world-renowned sportswear brand...</p>",
    "logo": {
      "id": "01ARZ9NDE...",
      "name": "Nike Logo",
      "url": "https://cdn.example.com/brands/nike-logo.png",
      "altText": "Nike brand logo"
    },
    "active": true,
    "createdAt": "2026-05-01T10:00:00Z"
  },
  "message": "Brand created successfully"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| BAD_REQUEST | Name is required | name is blank |
| BAD_REQUEST | Name exceeds 100 characters | name.length() > 100 |
| BAD_REQUEST | Description exceeds 2000 characters | description.length() > 2000 |
| BAD_REQUEST | Slug already exists | slug not unique |
| NOT_FOUND | Media not found | logoMediaId not found |
| BAD_REQUEST | Media must be an image | Media.type != IMAGE |

---

## 3.2 List Brands
**Endpoint:** `GET /api/admin/v1/brands`
**Security:** Required (JWT Bearer token, type="admin")

### Query Parameters
| Param | Type | Default | Rules |
|-------|------|---------|-------|
| limit | Integer | 20 | Min 1, Max 100 |
| offset | Integer | 0 | Min 0 |
| active | Boolean | null | Filter by active status |
| search | String | null | Search in name (LIKE %search%) |
| sort | String | "name,asc" | Sort field and direction |

### Process
1. Check Redis cache if no filters
2. Build query with BaseRepository
3. Apply filters
4. Return paginated brands

### Cache Strategy
```java
String cacheKey = "brands:list:" + MD5(JSON.of(active, search, sort, limit, offset));
String cached = redis.get(cacheKey);
if (cached != null) return parseJson(cached);
```

### DB Actions
- **Select:** Brand WHERE deleted = false
- **Count:** Total matching brands

### Response (200)
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "01ARZ3NDE...",
        "name": "Nike",
        "slug": "nike",
        "logo": {
          "id": "01ARZ9NDE...",
          "name": "Nike Logo",
          "url": "https://cdn.example.com/brands/nike-logo.png",
          "altText": "Nike brand logo"
        },
        "active": true
      },
      {
        "id": "01ARZ4NDE...",
        "name": "Adidas",
        "slug": "adidas",
        "logo": null,
        "active": true
      }
    ],
    "total": 25,
    "limit": 20,
    "offset": 0
  }
}
```

---

## 3.3 Get Single Brand
**Endpoint:** `GET /api/admin/v1/brands/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Path Parameters
| Param | Type | Required | Rules |
|-------|------|----------|-------|
| id | String | Yes | Valid Brand ULID |

### Process
1. Find Brand by ID
2. Return full brand details

### DB Actions
- **Select:** Brand WHERE id = :id AND deleted = false

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "name": "Nike",
    "slug": "nike",
    "description": "<p>Nike is a world-renowned sportswear brand...</p>",
    "logo": {
      "id": "01ARZ9NDE...",
      "name": "Nike Logo",
      "url": "https://cdn.example.com/brands/nike-logo.png",
      "altText": "Nike brand logo",
      "width": 500,
      "height": 200
    },
    "active": true,
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-05-01T10:00:00Z"
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Brand not found | Brand not found or deleted |

---

## 3.4 Update Brand
**Endpoint:** `PUT /api/admin/v1/brands/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "name": "Nike Official",
  "description": "<p>Updated description</p>",
  "logoMediaId": "01ARZ10NDE...",
  "active": true
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| name | String | No | Max 100 chars |
| description | String | No | Max 2000 chars, HTML allowed |
| logoMediaId | String | No | Null to remove, or valid IMAGE media |
| active | Boolean | No | true or false |

### Process
1. Find Brand by ID
2. If name changed → regenerate slug (check uniqueness)
3. If logoMediaId changed:
   - Null → remove logo
   - Valid ID → verify Media exists and is IMAGE type
4. Update only provided fields
5. Invalidate `brands:list` cache
6. Return updated Brand

### HTML Sanitization
```java
// Use OWASP Java HTML Sanitizer or similar
import org.owasp.html.Sanitizers;

private String sanitizeHtml(String html) {
    if (html == null) return null;
    return Sanitizers.FORMATTING.sanitize(html);
    // Or allow more tags: Sanitizers.BLOCKS + Sanitizers.FORMATTING
}
```

### DB Actions
- **Update:** Brand (name, slug, description, logo_id, active)
- **Select:** Brand (if logo change requires logo entity for URL)

### Cache Invalidation
- **Delete:** brands:list:*

### Response (200)
```json
{
  "success": true,
  "data": { /* full updated Brand object */ },
  "message": "Brand updated successfully"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Brand not found | Brand not found |
| BAD_REQUEST | Slug already exists | new slug not unique |
| NOT_FOUND | Media not found | logoMediaId not found |
| BAD_REQUEST | Media must be an image | Media.type != IMAGE |

---

## 3.5 Delete Brand (Soft)
**Endpoint:** `DELETE /api/admin/v1/brands/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Path Parameters
| Param | Type | Required | Rules |
|-------|------|----------|-------|
| id | String | Yes | Valid Brand ULID |

### Query Parameters
| Param | Type | Default | Rules |
|-------|------|---------|-------|
| action | String | "unlink" | Options: "unlink", "block" |

### Process
1. Find Brand by ID
2. Check if brand has associated products:
   - If action = "unlink" → set Product.brand = null for all products using this brand
   - If action = "block" → return error (brand has products, use action=unlink to proceed)
3. Soft delete Brand (deleted = true)
4. Invalidate `brands:list` cache
5. Return success

### DB Actions
- **Update:** Brand SET deleted = true WHERE id = :id
- **Update:** Product SET brand_id = null WHERE brand_id = :id AND deleted = false (if unlink)

### Cache Invalidation
- **Delete:** brands:list:*

### Response (200)
```json
{
  "success": true,
  "message": "Brand deleted successfully",
  "data": {
    "unlinkedProducts": 15
  }
}
```

### Response (200) - Blocked
```json
{
  "success": true,
  "message": "Cannot delete brand with 15 associated products",
  "data": {
    "action": "block",
    "associatedProducts": 15
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Brand not found | Brand not found |
| BAD_REQUEST | Brand has associated products | action = "block" AND products exist |
| BAD_REQUEST | Invalid action | action not in ["unlink", "block"] |

---

## Notes for Junior Engineers

### Brand-Product Relationship
- Product → Brand is ManyToOne (nullable)
- Products can exist without a brand
- When deleting a brand, products should NOT be deleted
- Products retain their other data, just lose brand association

### Logo Display in Admin Panel
```java
public String getLogoUrl(Brand brand) {
    if (brand.getLogo() == null) return null;
    Media logo = brand.getLogo();
    if (logo.hasValidPresignedUrl()) {
        return logo.getPresignedUrl();
    }
    return logo.getUrl();
}
```

### Search Implementation
```java
Specification<Brand> searchByName(String search) {
    return (root, query, cb) -> {
        if (search == null || search.isBlank()) return null;
        return cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
    };
}
```

### Cache Key Pattern
```
brands:list:0-20-null-null-name,asc   → md5 hash of params
brands:list:*                          → wildcard for invalidation
```

### Why HTML Description is Allowed
- Admin can use rich text for brand story/about page
- Stored as-is, sanitized on output (in frontend or via API response filter)
- Frontend will render via `dangerouslySetInnerHTML` or similar

### Product Count Query
```java
public long getProductCount(String brandId) {
    return productRepository.countByBrandIdAndDeletedFalse(brandId);
}
```
