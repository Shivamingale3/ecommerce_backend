# Phase 2: Category Management

**Database Table:** Category (id, name, slug, description, displayOrder, active, path, depth, parent_id, image_id, deleted, createdAt, updatedAt, version)
**Related Tables:** Media (category.image = Media.id), ProductCategory (for cascade soft delete)
**Cache (Redis):** `categories:tree` - TTL 30min - full category tree

---

## 2.1 Create Category
**Endpoint:** `POST /api/admin/v1/categories`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "name": "Running Shoes",
  "parentCategoryId": null,
  "description": "High-performance running footwear",
  "imageMediaId": null,
  "displayOrder": 0
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| name | String | Yes | Max 100 chars, not blank |
| parentCategoryId | String | No | Must exist if provided |
| description | String | No | Max 500 chars |
| imageMediaId | String | No | Must exist and be IMAGE type if provided |
| displayOrder | Integer | No | Default 0, >= 0 |

### Process
1. Validate name is not blank, max 100 chars
2. Validate slug uniqueness (auto-generate from name)
3. If parentCategoryId provided:
   - Find parent Category
   - Calculate depth = parent.depth + 1
   - Calculate path = parent.path + "/" + slug
   - Validate depth <= 3 (optional limit)
4. If parentCategoryId null:
   - depth = 0
   - path = slug
5. If imageMediaId provided:
   - Verify Media exists, type == IMAGE
   - Link Category.image → Media
6. Create Category with auto-generated slug
7. Invalidate `categories:tree` cache
8. Return Category

### Slug Generation
```java
public String generateSlug(String name) {
    String slug = name.toLowerCase()
        .replaceAll("[^a-z0-9\\s-]", "")
        .replaceAll("\\s+", "-")
        .replaceAll("-+", "-")
        .trim();
    return slug;
}
```

### DB Actions
- **Insert:** Category (name, slug, description, displayOrder, active, path, depth, parent_id, image_id)
- **Check:** parentCategory exists if provided
- **Check:** imageMedia exists if provided

### Redis Actions
- **Delete:** categories:tree

### Response (201)
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "name": "Running Shoes",
    "slug": "running-shoes",
    "description": "High-performance running footwear",
    "displayOrder": 0,
    "active": true,
    "path": "running-shoes",
    "depth": 0,
    "parent": null,
    "children": [],
    "image": null
  },
  "message": "Category created successfully"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| BAD_REQUEST | Name is required | name is blank |
| BAD_REQUEST | Name exceeds 100 characters | name.length() > 100 |
| BAD_REQUEST | Description exceeds 500 characters | description.length() > 500 |
| NOT_FOUND | Parent category not found | parentCategoryId not found |
| BAD_REQUEST | Slug already exists | slug not unique |
| BAD_REQUEST | Maximum depth exceeded | depth > 3 (if limit set) |
| NOT_FOUND | Media not found | imageMediaId not found |
| BAD_REQUEST | Media must be an image | Media.type != IMAGE |

---

## 2.2 List Categories
**Endpoint:** `GET /api/admin/v1/categories`
**Security:** Required (JWT Bearer token, type="admin")

### Query Parameters
| Param | Type | Default | Rules |
|-------|------|---------|-------|
| limit | Integer | 50 | Min 1, Max 200 |
| offset | Integer | 0 | Min 0 |
| active | Boolean | null | Filter by active status |
| parentId | String | null | Filter by parent (null = only root categories) |

### Process
1. Check Redis cache `categories:tree`
2. If cache exists and no filters → return cached tree
3. Build query with BaseRepository findAllByDeletedFalse
4. Apply active and parentId filters
5. Sort by displayOrder ASC, name ASC
6. Build tree structure in memory:
   - Group by parent_id
   - Build parent.children list
7. Return flat list OR nested tree based on `format=tree` query param

### Cache Strategy
```java
// If no filters (all categories), return cached tree
if (active == null && parentId == null) {
    String cached = redis.get("categories:tree");
    if (cached != null) return parseJson(cached);
}

// After fetching fresh data
if (active == null && parentId == null) {
    redis.setex("categories:tree", 1800, json); // 30 min TTL
}
```

### DB Actions
- **Select:** Category WHERE deleted = false
- **Select:** Category WHERE parent_id = :parentId AND deleted = false

### Response (200) - Flat List
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "01ARZ3NDE...",
        "name": "Running Shoes",
        "slug": "running-shoes",
        "description": "High-performance running footwear",
        "displayOrder": 0,
        "active": true,
        "path": "running-shoes",
        "depth": 0,
        "parent": null,
        "image": { "id": "...", "name": "Category Banner", "url": "..." }
      },
      {
        "id": "01ARZ4NDE...",
        "name": "Trail Running",
        "slug": "trail-running",
        "displayOrder": 1,
        "active": true,
        "path": "running-shoes/trail-running",
        "depth": 1,
        "parent": { "id": "01ARZ3NDE...", "name": "Running Shoes" }
      }
    ],
    "total": 25,
    "limit": 50,
    "offset": 0
  }
}
```

### Response (200) - Tree Format
```json
{
  "success": true,
  "data": {
    "categories": [
      {
        "id": "01ARZ3NDE...",
        "name": "Running Shoes",
        "slug": "running-shoes",
        "displayOrder": 0,
        "active": true,
        "children": [
          {
            "id": "01ARZ4NDE...",
            "name": "Trail Running",
            "slug": "trail-running",
            "displayOrder": 0,
            "active": true,
            "children": []
          },
          {
            "id": "01ARZ5NDE...",
            "name": "Road Running",
            "slug": "road-running",
            "displayOrder": 1,
            "active": true,
            "children": []
          }
        ]
      },
      {
        "id": "01ARZ6NDE...",
        "name": "Casual Shoes",
        "slug": "casual-shoes",
        "displayOrder": 1,
        "active": true,
        "children": []
      }
    ],
    "total": 25
  }
}
```

---

## 2.3 Get Single Category
**Endpoint:** `GET /api/admin/v1/categories/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Path Parameters
| Param | Type | Required | Rules |
|-------|------|----------|-------|
| id | String | Yes | Valid Category ULID |

### Process
1. Find Category by ID (includes parent and children via LAZY fetch with @ToString exclude)
2. Return full category with computed path

### DB Actions
- **Select:** Category WHERE id = :id AND deleted = false

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "name": "Running Shoes",
    "slug": "running-shoes",
    "description": "High-performance running footwear",
    "displayOrder": 0,
    "active": true,
    "path": "running-shoes",
    "depth": 0,
    "parent": null,
    "children": [
      {
        "id": "01ARZ4NDE...",
        "name": "Trail Running",
        "slug": "trail-running",
        "displayOrder": 0,
        "active": true
      }
    ],
    "image": {
      "id": "01ARZ7NDE...",
      "name": "Running Banner",
      "url": "https://cdn.example.com/media/running-banner.jpg"
    },
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-05-01T10:00:00Z"
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Category not found | Category not found or deleted |

---

## 2.4 Update Category
**Endpoint:** `PUT /api/admin/v1/categories/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "name": "Performance Running Shoes",
  "parentCategoryId": "01ARZ8NDE...",
  "description": "Updated description",
  "imageMediaId": "01ARZ9NDE...",
  "active": true
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| name | String | No | Max 100 chars |
| parentCategoryId | String | No | Null to make root, or valid parent |
| description | String | No | Max 500 chars |
| imageMediaId | String | No | Null to remove, or valid IMAGE media |
| active | Boolean | No | true or false |

### Process
1. Find Category by ID
2. If name changed → regenerate slug (check uniqueness)
3. If parentCategoryId changed:
   - If null → set as root (depth = 0, path = slug)
   - If set → validate parent exists, not circular (category can't be its own ancestor)
   - Recalculate depth and path for this AND all children
4. If imageMediaId changed:
   - Verify new media exists and is IMAGE type
   - Or set null to remove image
5. Update fields
6. Save Category
7. Invalidate `categories:tree` cache
8. Return updated Category

### Circular Reference Check
```java
private boolean isCircularReference(String categoryId, String newParentId) {
    Category current = categoryRepository.findById(newParentId);
    while (current != null && current.getParent() != null) {
        if (current.getParent().getId().equals(categoryId)) {
            return true; // Circular!
        }
        current = current.getParent();
    }
    return false;
}
```

### Path Recalculation (for self and all descendants)
```java
public void recalculatePathAndDepth(Category category) {
    if (category.getParent() == null) {
        category.setDepth(0);
        category.setPath(category.getSlug());
    } else {
        category.setDepth(category.getParent().getDepth() + 1);
        category.setPath(category.getParent().getPath() + "/" + category.getSlug());
    }
    // Recursively update children
    for (Category child : category.getChildren()) {
        recalculatePathAndDepth(child);
    }
}
```

### DB Actions
- **Update:** Category (name, slug, description, displayOrder, active, path, depth, parent_id, image_id)
- **Select:** parentCategory (if provided)
- **Select:** Media (if imageMediaId provided)

### Redis Actions
- **Delete:** categories:tree

### Response (200)
```json
{
  "success": true,
  "data": { /* full updated Category object */ },
  "message": "Category updated successfully"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Category not found | Category not found |
| NOT_FOUND | Parent category not found | parentCategoryId not found |
| BAD_REQUEST | Circular reference detected | category is ancestor of new parent |
| BAD_REQUEST | Slug already exists | new slug not unique |
| BAD_REQUEST | Name exceeds 100 characters | name.length() > 100 |
| NOT_FOUND | Media not found | imageMediaId not found |

---

## 2.5 Reorder Categories
**Endpoint:** `PUT /api/admin/v1/categories/reorder`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "orders": [
    { "categoryId": "01ARZ3NDE...", "displayOrder": 0 },
    { "categoryId": "01ARZ4NDE...", "displayOrder": 1 },
    { "categoryId": "01ARZ5NDE...", "displayOrder": 2 }
  ]
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| orders | Array | Yes | Min 1 item |
| orders[].categoryId | String | Yes | Must exist |
| orders[].displayOrder | Integer | Yes | >= 0 |

### Process
1. Validate all categoryIds exist
2. Batch update displayOrder for each
3. Return success

### DB Actions
- **Update:** Category SET displayOrder = :order WHERE id = :categoryId (batch)

### Response (200)
```json
{
  "success": true,
  "message": "Categories reordered successfully",
  "data": {
    "updatedCount": 3
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| BAD_REQUEST | At least one order entry required | orders array empty |
| NOT_FOUND | Category not found | One or more categoryIds not found |

---

## 2.6 Delete Category
**Endpoint:** `DELETE /api/admin/v1/categories/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Path Parameters
| Param | Type | Required | Rules |
|-------|------|----------|-------|
| id | String | Yes | Valid Category ULID |

### Query Parameters
| Param | Type | Default | Rules |
|-------|------|---------|-------|
| cascade | Boolean | false | If true, delete children too |

### Process
1. Find Category by ID
2. Check if has children:
   - If cascade = true → soft delete all children recursively
   - If cascade = false → return error (has children)
3. Soft delete all linked ProductCategory records (ProductCategory.deleted = true)
4. Soft delete Category (deleted = true)
5. Invalidate `categories:tree` cache
6. Return success

### DB Actions
- **Update:** Category SET deleted = true WHERE id = :id
- **Update:** Category SET deleted = true WHERE parent_id = :id (if cascade)
- **Update:** ProductCategory SET deleted = true WHERE category_id = :id
- **Update:** Category SET deleted = true WHERE path LIKE :path% (if cascade, all descendants)

### Redis Actions
- **Delete:** categories:tree

### Response (200)
```json
{
  "success": true,
  "message": "Category deleted successfully"
}
```

### Response (200) - With children warning
```json
{
  "success": true,
  "message": "Category deleted, 3 child categories also deleted",
  "data": {
    "deletedCategories": 4,
    "unlinkedProducts": 15
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Category not found | Category not found |
| BAD_REQUEST | Cannot delete category with children | cascade = false AND has children |

---

## 2.7 Set Category Image
**Endpoint:** `PUT /api/admin/v1/categories/{id}/image`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "mediaId": "01ARZ9NDE..."
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| mediaId | String | No | Null to remove, or valid IMAGE media |

### Process
1. Find Category by ID
2. If mediaId null → set image to null
3. If mediaId provided → verify Media exists and type == IMAGE
4. Update Category.image
5. Invalidate `categories:tree` cache
6. Return updated Category

### DB Actions
- **Update:** Category SET image_id = :mediaId WHERE id = :id
- **Select:** Media (to verify type)

### Response (200)
```json
{
  "success": true,
  "data": { /* full Category object */ },
  "message": "Category image updated"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Category not found | Category not found |
| NOT_FOUND | Media not found | mediaId not found |
| BAD_REQUEST | Media must be an image | Media.type != IMAGE |

---

## Notes for Junior Engineers

### Category Path Format
```
Level 0: "shoes"
Level 1: "shoes/running-shoes"
Level 2: "shoes/running-shoes/trail-running"
Level 3: "shoes/running-shoes/trail-running/racing-flats"
```

### Why Path is Denormalized
- Avoids recursive queries for breadcrumb display
- Enables LIKE queries for "all products in this category and subcategories"
- Example: `WHERE category_path LIKE 'shoes/running-shoes%'`

### Tree Building Algorithm
```java
public List<CategoryDTO> buildTree(List<Category> flatList) {
    Map<String, List<Category>> childrenMap = flatList.stream()
        .filter(c -> c.getParent() != null)
        .collect(Collectors.groupingBy(c -> c.getParent().getId()));

    return flatList.stream()
        .filter(c -> c.getParent() == null) // Root categories
        .map(c -> buildTreeNode(c, childrenMap))
        .collect(Collectors.toList());
}

private CategoryDTO buildTreeNode(Category category, Map<String, List<Category>> childrenMap) {
    CategoryDTO dto = CategoryDTO.fromEntity(category);
    List<Category> children = childrenMap.getOrDefault(category.getId(), Collections.emptyList());
    dto.setChildren(children.stream()
        .map(c -> buildTreeNode(c, childrenMap))
        .collect(Collectors.toList()));
    return dto;
}
```

### Cache Invalidation Pattern
```java
@CacheEvict(value = "categories", allEntries = true)
public Category save(Category category) {
    // Save logic
}
```

### Media Link Query
```java
// Verify media exists and is IMAGE type before linking
public Optional<Media> validateImageMedia(String mediaId) {
    return mediaRepository.findByIdAndDeletedFalse(mediaId)
        .filter(m -> m.getType() == MediaType.IMAGE);
}
```

### Display Order Defaults
- New categories get displayOrder = 0 (appears first)
- When reordering, use 0, 1, 2, 3...
- Gaps in sequence are fine for future insertions
