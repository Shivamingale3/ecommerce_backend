# Phase 4: Tag Management

**Database Table:** Tag (id, name, slug, deleted, createdAt, updatedAt, version)
**Related Tables:** ProductTag (product_id, tag_id)

---

## 4.1 Create Tag
**Endpoint:** `POST /api/admin/v1/tags`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "name": "Best Seller"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| name | String | Yes | Max 50 chars, not blank |

### Process
1. Validate name (not blank, max 50 chars)
2. Generate slug from name
3. Check slug uniqueness
4. Create Tag
5. Return Tag

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
- **Insert:** Tag (name, slug)

### Response (201)
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "name": "Best Seller",
    "slug": "best-seller"
  },
  "message": "Tag created successfully"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| BAD_REQUEST | Name is required | name is blank |
| BAD_REQUEST | Name exceeds 50 characters | name.length() > 50 |
| BAD_REQUEST | Tag already exists | slug already exists |

---

## 4.2 List Tags
**Endpoint:** `GET /api/admin/v1/tags`
**Security:** Required (JWT Bearer token, type="admin")

### Query Parameters
| Param | Type | Default | Rules |
|-------|------|---------|-------|
| limit | Integer | 50 | Min 1, Max 200 |
| offset | Integer | 0 | Min 0 |
| search | String | null | Search in name (LIKE %search%) |

### Process
1. Build query with BaseRepository
2. Apply search filter if provided
3. Return paginated tags

### DB Actions
- **Select:** Tag WHERE deleted = false
- **Count:** Total matching tags

### Response (200)
```json
{
  "success": true,
  "data": {
    "items": [
      { "id": "01ARZ3NDE...", "name": "Best Seller", "slug": "best-seller" },
      { "id": "01ARZ4NDE...", "name": "New Arrival", "slug": "new-arrival" },
      { "id": "01ARZ5NDE...", "name": "Summer Collection", "slug": "summer-collection" }
    ],
    "total": 50,
    "limit": 50,
    "offset": 0
  }
}
```

---

## 4.3 Update Tag
**Endpoint:** `PUT /api/admin/v1/tags/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "name": "Top Rated"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| name | String | Yes | Max 50 chars, not blank |

### Process
1. Find Tag by ID
2. Validate new name
3. If name changed → regenerate slug (check uniqueness)
4. Update Tag
5. Return updated Tag

### DB Actions
- **Update:** Tag (name, slug) WHERE id = :id

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "name": "Top Rated",
    "slug": "top-rated"
  },
  "message": "Tag updated successfully"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Tag not found | Tag not found |
| BAD_REQUEST | Tag already exists | new slug already exists |

---

## 4.4 Delete Tag (Soft)
**Endpoint:** `DELETE /api/admin/v1/tags/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Path Parameters
| Param | Type | Required | Rules |
|-------|------|----------|-------|
| id | String | Yes | Valid Tag ULID |

### Process
1. Find Tag by ID
2. Soft delete ProductTag records linking this tag
3. Soft delete Tag (deleted = true)
4. Return success

### DB Actions
- **Update:** Tag SET deleted = true WHERE id = :id
- **Update:** ProductTag SET deleted = true WHERE tag_id = :id

### Response (200)
```json
{
  "success": true,
  "message": "Tag deleted successfully",
  "data": {
    "unlinkedProducts": 8
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Tag not found | Tag not found |

---

## 4.5 Create Tag Inline (During Product Edit)
**Endpoint:** `POST /api/admin/v1/tags/inline`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "name": "Flash Sale"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| name | String | Yes | Max 50 chars, not blank |

### Process
1. Validate name
2. Generate slug
3. Check if slug exists:
   - If exists → return existing Tag (idempotent)
   - If not exists → create and return new Tag
4. Return Tag

### DB Actions
- **Select:** Tag WHERE slug = :slug AND deleted = false
- **Insert:** Tag (if not exists)

### Response (201) - Created New
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "name": "Flash Sale",
    "slug": "flash-sale",
    "created": true
  },
  "message": "Tag created"
}
```

### Response (200) - Already Existed
```json
{
  "success": true,
  "data": {
    "id": "01ARZ4NDE...",
    "name": "Flash Sale",
    "slug": "flash-sale",
    "created": false
  },
  "message": "Tag already exists"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| BAD_REQUEST | Name exceeds 50 characters | name.length() > 50 |

### Usage Note
This endpoint is called when:
1. Admin is creating/editing a product
2. Admin types a new tag name that doesn't exist
3. Frontend calls this to create tag on-the-fly
4. Frontend then uses the returned tagId in the product relations update

---

## Notes for Junior Engineers

### Tag vs Category
| Aspect | Tag | Category |
|--------|-----|----------|
| Hierarchy | Flat (no parent/child) | Hierarchical (parent/child) |
| Slug | Auto-generated | Auto-generated |
| Display in Product | Multiple tags per product | Multiple categories, one primary |
| Use Case | Marketing labels (Best Seller, New, Sale) | Navigation/browsing structure |

### ProductTag Cleanup on Tag Delete
When deleting a tag, all ProductTag records linking to it should also be soft-deleted. This prevents orphaned junction records.

### Idempotent Inline Creation
The inline creation is idempotent - calling it with the same name twice returns the same tag. This is important for:
- Frontend auto-complete showing existing tags
- Admin typing a tag that already exists
- Concurrent requests from multiple admin sessions
