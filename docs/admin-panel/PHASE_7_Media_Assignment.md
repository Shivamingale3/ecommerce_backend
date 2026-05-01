# Phase 7: Media (Product Assignment View)

**Database Tables:** Media, ProductMedia

---

## 7.1 Get Unassigned Media
**Endpoint:** `GET /api/admin/v1/media/unassigned`
**Security:** Required (JWT Bearer token, type="admin")

### Query Parameters
| Param | Type | Default | Rules |
|-------|------|---------|-------|
| limit | Integer | 20 | Min 1, Max 100 |
| offset | Integer | 0 | Min 0 |
| type | String | null | Filter by MediaType |

### Process
1. Query Media that is not in any active ProductMedia record
2. Return paginated list

### DB Actions
- **Select:** Media M WHERE M.deleted = false AND NOT EXISTS (SELECT 1 FROM ProductMedia PM WHERE PM.media_id = M.id AND PM.deleted = false)

### Response (200)
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "01ARZMEDIA...",
        "name": "Unused Image",
        "type": "IMAGE",
        "url": "https://cdn.example.com/...",
        "altText": "Unused product image"
      }
    ],
    "total": 50,
    "limit": 20,
    "offset": 0
  }
}
```

---

## 7.2 Get Media by Role for Product
**Endpoint:** `GET /api/admin/v1/products/{id}/media`
**Security:** Required (JWT Bearer token, type="admin")

### Query Parameters
| Param | Type | Default | Rules |
|-------|------|---------|-------|
| role | String | null | Filter by MediaRole (MAIN, COVER, GRID, THUMBNAIL, GALLERY, REVIEW) |

### Process
1. Find Product
2. Query ProductMedia for this product
3. Apply role filter if provided
4. Return media with roles

### DB Actions
- **Select:** ProductMedia PM WHERE PM.product_id = :id AND PM.deleted = false
- **Join:** Media for each ProductMedia

### Response (200)
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "01ARZPM1...",
        "media": {
          "id": "01ARZMEDIA...",
          "name": "Hero Image",
          "url": "https://cdn.example.com/...",
          "type": "IMAGE",
          "width": 1920,
          "height": 1080
        },
        "role": "COVER",
        "displayOrder": 0,
        "altTextOverride": "Nike Air Max 90 Front"
      },
      {
        "id": "01ARZPM2...",
        "media": {
          "id": "01ARZMEDIA2...",
          "name": "Side View",
          "url": "https://cdn.example.com/...",
          "type": "IMAGE"
        },
        "role": "GALLERY",
        "displayOrder": 1
      }
    ],
    "total": 5
  }
}
```

---

## Summary of All Endpoints

### Authentication (Phase 0)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/admin/v1/auth/login | Request OTP |
| POST | /api/admin/v1/auth/verify-otp | Verify OTP, get tokens |
| POST | /api/admin/v1/auth/resend-otp | Resend OTP |
| POST | /api/admin/v1/auth/refresh | Refresh tokens |
| POST | /api/admin/v1/auth/logout | Logout |
| GET | /api/admin/v1/profile | Get profile |
| PUT | /api/admin/v1/profile | Update profile |

### Media (Phase 1)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/admin/v1/media/upload-url | Get presigned upload URL |
| POST | /api/admin/v1/media/confirm-upload | Confirm upload, create record |
| GET | /api/admin/v1/media | List media |
| GET | /api/admin/v1/media/{id} | Get single media |
| PUT | /api/admin/v1/media/{id} | Update media |
| DELETE | /api/admin/v1/media/{id} | Delete media |
| GET | /api/admin/v1/media/{id}/url | Get/refresh presigned URL |
| GET | /api/admin/v1/media/unassigned | Get unassigned media |

### Categories (Phase 2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/admin/v1/categories | Create category |
| GET | /api/admin/v1/categories | List categories |
| GET | /api/admin/v1/categories/{id} | Get single category |
| PUT | /api/admin/v1/categories/{id} | Update category |
| PUT | /api/admin/v1/categories/reorder | Reorder categories |
| DELETE | /api/admin/v1/categories/{id} | Delete category |
| PUT | /api/admin/v1/categories/{id}/image | Set category image |

### Brands (Phase 3)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/admin/v1/brands | Create brand |
| GET | /api/admin/v1/brands | List brands |
| GET | /api/admin/v1/brands/{id} | Get single brand |
| PUT | /api/admin/v1/brands/{id} | Update brand |
| DELETE | /api/admin/v1/brands/{id} | Delete brand |

### Tags (Phase 4)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/admin/v1/tags | Create tag |
| POST | /api/admin/v1/tags/inline | Create tag inline |
| GET | /api/admin/v1/tags | List tags |
| PUT | /api/admin/v1/tags/{id} | Update tag |
| DELETE | /api/admin/v1/tags/{id} | Delete tag |

### Products (Phase 5)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/admin/v1/products | Create product |
| GET | /api/admin/v1/products | List products |
| GET | /api/admin/v1/products/{id} | Get product detail |
| PUT | /api/admin/v1/products/{id} | Update basic info |
| PUT | /api/admin/v1/products/{id}/relations | Assign categories/tags |
| PUT | /api/admin/v1/products/{id}/media | Assign media |
| PUT | /api/admin/v1/products/{id}/pricing | Set pricing |
| PUT | /api/admin/v1/products/{id}/inventory | Update inventory |
| PUT | /api/admin/v1/products/{id}/shipping | Set shipping/dimensions |
| PUT | /api/admin/v1/products/{id}/seo | Set SEO fields |
| PUT | /api/admin/v1/products/{id}/visibility | Set featured/visibility |
| PUT | /api/admin/v1/products/{id}/status | Change status |
| DELETE | /api/admin/v1/products/{id} | Delete product |
| PUT | /api/admin/v1/products/bulk/status | Bulk status change |
| DELETE | /api/admin/v1/products/bulk | Bulk delete |
| GET | /api/admin/v1/products/{id}/media | Get product media by role |

### Inventory (Phase 6)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/admin/v1/inventory/low-stock | List low stock |
| PUT | /api/admin/v1/inventory/bulk | Bulk stock update |
| POST | /api/admin/v1/inventory/{productId}/adjust | Stock adjustment |
| POST | /api/admin/v1/inventory/{productId}/reserve | Reserve stock |