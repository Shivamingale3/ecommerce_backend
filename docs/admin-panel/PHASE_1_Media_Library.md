# Phase 1: Media Library

**Database Table:** Media (id, name, type, key, url, presignedUrl, presignedUrlExpiresAt, altText, fileSize, mimeType, width, height, active, deleted, createdAt, updatedAt, version)
**Cache (Redis):**
- `pending_upload:{mediaId}` - TTL 5min - stores temp key for abandoned upload cleanup
- `media:url:{id}` - TTL 55min - stores presigned GET URL for quick retrieval

**S3/MinIO Integration:** ObjectStorageService (existing)

---

## 1.1 Request Upload URL
**Endpoint:** `POST /api/admin/v1/media/upload-url`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "filename": "hero-image.jpg",
  "mimeType": "image/jpeg",
  "fileSize": 2048000
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| filename | String | Yes | Max 255 chars, must have valid extension |
| mimeType | String | Yes | Must be: image/jpeg, image/png, image/webp, image/gif, video/mp4, video/webm, audio/mpeg, audio/ogg, application/pdf |
| fileSize | Long | Yes | Max 5MB = 5242880 bytes, min 1 byte |

### Process
1. Validate fileSize <= 5MB
2. Validate mimeType is allowed
3. Generate unique S3 key: `media/{timestamp}/{uuid}/{filename}`
4. Generate presigned PUT URL from ObjectStorageService (expires 5min)
5. Generate temp mediaId: `temp_{ulid}`
6. Store in Redis: `pending_upload:{temp_mediaId}` = {key, createdAt, filename, mimeType} with TTL 5min
7. Return uploadUrl and temp mediaId

### DB Actions
- None (not yet created)

### S3 Actions
- Generate presigned PUT URL for key

### Response (200)
```json
{
  "success": true,
  "data": {
    "mediaId": "temp_01ARZ3NDE...",
    "uploadUrl": "https://minio.example.com/bucket/media/2026/05/01/01ARZ3NDE/hero-image.jpg?X-Amz-Signature=...",
    "key": "media/2026/05/01/01ARZ3NDE/hero-image.jpg",
    "expiresAt": "2026-05-01T10:05:00Z",
    "method": "PUT"
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| BAD_REQUEST | File size exceeds 5MB limit | fileSize > 5242880 |
| BAD_REQUEST | File type not allowed | mimeType not in allowed list |
| BAD_REQUEST | Invalid filename | Missing or invalid extension |

### Allowed MIME Types
```java
Set<String> ALLOWED_MIME_TYPES = Set.of(
    "image/jpeg", "image/png", "image/webp", "image/gif",
    "video/mp4", "video/webm",
    "audio/mpeg", "audio/ogg",
    "application/pdf"
);
```

---

## 1.2 Confirm Upload
**Endpoint:** `POST /api/admin/v1/media/confirm-upload`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "mediaId": "temp_01ARZ3NDE...",
  "name": "Product Hero Image",
  "type": "IMAGE",
  "altText": "Nike Air Max running shoe正面展示"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| mediaId | String | Yes | Must start with "temp_", must exist in Redis |
| name | String | Yes | Max 100 chars |
| type | Enum | Yes | Must be: IMAGE, VIDEO, AUDIO, DOCUMENT |
| altText | String | No | Max 300 chars |

### Process
1. Extract tempMediaId from request
2. Check Redis for `pending_upload:{tempMediaId}`
3. If not found → error (upload abandoned or already confirmed)
4. Verify S3 object exists at key (optional: using headObject)
5. Create Media record with:
   - name = request.name
   - type = request.type
   - key = Redis.key
   - url = public URL (assemble from bucket + key)
   - altText = request.altText
   - fileSize = Redis.fileSize
   - mimeType = Redis.mimeType
   - active = true
6. Delete Redis key `pending_upload:{tempMediaId}`
7. Generate initial presigned GET URL and store in DB (expires 1 hour)
8. Cache presigned URL in Redis
9. Return Media record

### DB Actions
- **Insert:** Media (name, type, key, url, altText, fileSize, mimeType, active, presignedUrl, presignedUrlExpiresAt)

### Redis Actions
- **Get:** pending_upload:{tempMediaId}
- **Delete:** pending_upload:{tempMediaId}
- **Set:** media:url:{mediaId} with presigned URL, TTL 55min

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "name": "Product Hero Image",
    "type": "IMAGE",
    "key": "media/2026/05/01/01ARZ3NDE/hero-image.jpg",
    "url": "https://cdn.example.com/media/2026/05/01/01ARZ3NDE/hero-image.jpg",
    "presignedUrl": "https://minio.example.com/bucket/media/2026/05/01/01ARZ3NDE/hero-image.jpg?X-Amz-Signature=...",
    "presignedUrlExpiresAt": "2026-05-01T11:00:00Z",
    "altText": "Nike Air Max running shoe正面展示",
    "fileSize": 2048000,
    "mimeType": "image/jpeg",
    "width": 1920,
    "height": 1080,
    "active": true
  },
  "message": "Media uploaded and saved successfully"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Upload not found or expired | Redis key not found |
| BAD_REQUEST | Invalid media ID format | mediaId doesn't start with "temp_" |
| SERVICE_ERROR | Failed to verify upload | S3 headObject fails |

### Post-Submit Image Processing (Async - Future Enhancement)
- After confirm, queue job to:
  - Get image dimensions using Java image API or ImageIO
  - Update Media.width and Media.height
  - Generate thumbnail if VIDEO
  - Scan for EXIF data

---

## 1.3 List Media
**Endpoint:** `GET /api/admin/v1/media`
**Security:** Required (JWT Bearer token, type="admin")

### Query Parameters
| Param | Type | Default | Rules |
|-------|------|---------|-------|
| limit | Integer | 20 | Min 1, Max 100 |
| offset | Integer | 0 | Min 0 |
| type | String | null | Filter by MediaType (IMAGE, VIDEO, AUDIO, DOCUMENT) |
| active | Boolean | null | Filter by active status |
| search | String | null | Search in name (LIKE %search%) |

### Process
1. Build pageable query
2. Apply filters
3. Return paginated list

### DB Actions
- **Select:** Media WHERE deleted = false (handled by BaseRepository)
- **Count:** Total matching records

### Response (200)
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "01ARZ3NDE...",
        "name": "Product Hero Image",
        "type": "IMAGE",
        "url": "https://cdn.example.com/media/2026/05/01/hero-image.jpg",
        "altText": "Nike Air Max",
        "fileSize": 2048000,
        "mimeType": "image/jpeg",
        "width": 1920,
        "height": 1080,
        "active": true
      }
    ],
    "total": 150,
    "limit": 20,
    "offset": 0
  }
}
```

---

## 1.4 Get Single Media
**Endpoint:** `GET /api/admin/v1/media/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Process
1. Find Media by ID
2. Return full record

### DB Actions
- **Select:** Media WHERE id = :id AND deleted = false

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "name": "Product Hero Image",
    "type": "IMAGE",
    "key": "media/2026/05/01/hero-image.jpg",
    "url": "https://cdn.example.com/media/2026/05/01/hero-image.jpg",
    "presignedUrl": "https://minio.example.com/bucket/...",
    "presignedUrlExpiresAt": "2026-05-01T11:00:00Z",
    "altText": "Nike Air Max",
    "fileSize": 2048000,
    "mimeType": "image/jpeg",
    "width": 1920,
    "height": 1080,
    "active": true,
    "createdAt": "2026-05-01T10:00:00Z",
    "updatedAt": "2026-05-01T10:00:00Z"
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Media not found | Media not found or deleted |

---

## 1.5 Update Media
**Endpoint:** `PUT /api/admin/v1/media/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "name": "Updated Hero Image",
  "altText": "Updated alt text",
  "active": true
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| name | String | No | Max 100 chars |
| altText | String | No | Max 300 chars |
| active | Boolean | No | true or false |

### Process
1. Find Media by ID
2. Update only provided fields
3. If altText changed → invalidate cached presigned URL
4. Save
5. Return updated record

### DB Actions
- **Update:** Media (name, altText, active) WHERE id = :id

### Redis Actions
- **Delete:** media:url:{id} (invalidate cache if altText changed)

### Response (200)
```json
{
  "success": true,
  "data": { /* full Media object */ },
  "message": "Media updated successfully"
}
```

---

## 1.6 Delete Media (Soft)
**Endpoint:** `DELETE /api/admin/v1/media/{id}`
**Security:** Required (JWT Bearer token, type="admin")

### Process
1. Find Media by ID
2. Check if media is used in any active ProductMedia records
3. If used → warn (still allow soft delete)
4. Set deleted = true
5. Delete Redis cache keys
6. (Optional) Do NOT delete S3 object - keep for recovery

### DB Actions
- **Update:** Media SET deleted = true WHERE id = :id
- **Check:** ProductMedia WHERE media_id = :id AND deleted = false

### Redis Actions
- **Delete:** media:url:{id}
- **Delete:** pending_upload:{id} (if exists)

### Response (200)
```json
{
  "success": true,
  "message": "Media deleted successfully"
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Media not found | Media not found or already deleted |

### Warning Response (if media is in use)
```json
{
  "success": true,
  "message": "Media deleted but is still referenced by 3 products",
  "warning": true
}
```

---

## 1.7 Get/Refresh Presigned URL
**Endpoint:** `GET /api/admin/v1/media/{id}/url`
**Security:** Required (JWT Bearer token, type="admin")

### Query Parameters
| Param | Type | Default | Rules |
|-------|------|---------|-------|
| refresh | Boolean | false | Force regenerate URL |

### Process
1. Find Media by ID
2. If refresh == false AND media.hasValidPresignedUrl() AND url exists in Redis cache → return cached
3. If refresh == true OR no valid cached URL:
   - Generate new presigned GET URL (expires 1 hour)
   - Update Media.presignedUrl and presignedUrlExpiresAt
   - Update Redis cache (TTL 55min)
4. Return URL and expiresAt

### DB Actions
- **Select:** Media WHERE id = :id AND deleted = false
- **Update:** Media (presignedUrl, presignedUrlExpiresAt) WHERE id = :id (only if refreshing)

### Redis Actions
- **Get:** media:url:{id}
- **Set:** media:url:{id} = {url, expiresAt} TTL 55min

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "url": "https://minio.example.com/bucket/media/.../hero-image.jpg?X-Amz-Signature=...",
    "expiresAt": "2026-05-01T11:00:00Z",
    "cached": true
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Media not found | Media not found or deleted |

---

## 1.8 Pending Upload Cleanup (Scheduled Task)
**Type:** Scheduled Task / Event-driven
**Trigger:** Every 1 minute OR via Redis key expiry (recommended)

### Process (Redis TTL Method - Recommended)
1. Use Redis key expiry listener
2. When `pending_upload:{tempId}` expires (TTL 5min):
3. Check S3 if object exists at key
4. If exists → Delete S3 object (abandoned upload)
5. Log cleanup action

### Process (Scheduled Job Alternative)
1. Query Redis for all `pending_upload:*` keys
2. Check createdAt > 5 minutes ago
3. Delete S3 objects for expired keys
4. Delete Redis keys

### Logging
```java
log.info("Cleaned up abandoned upload: key={}, filename={}", key, filename);
```

---

## Redis Key Patterns Reference

| Key Pattern | Value | TTL | Purpose |
|-------------|-------|-----|---------|
| `pending_upload:temp_{id}` | `{key, filename, mimeType, fileSize, createdAt}` | 5min | Track initiated but unconfirmed uploads |
| `media:url:{id}` | `{presignedUrl, expiresAt}` | 55min | Cache presigned GET URLs |

---

## Notes for Junior Engineers

### S3 Key Format
```
media/{year}/{month}/{day}/{ulid}/{original_filename}
Example: media/2026/05/01/01ARZ3NDE5ABC123DEF/hero-image.jpg
```

### CDN URL Format
```
https://cdn.example.com/{key}
Example: https://cdn.example.com/media/2026/05/01/01ARZ3NDE5ABC123DEF/hero-image.jpg
```

### ObjectStorageService Usage
```java
// Generate presigned PUT URL (for upload)
String uploadUrl = objectStorageService.generatePresignedPutUrl(key, contentType, expiresInSeconds);

// Generate presigned GET URL (for access)
String getUrl = objectStorageService.generatePresignedGetUrl(key, expiresInSeconds);

// Check if object exists
boolean exists = objectStorageService.objectExists(key);

// Delete object
objectStorageService.deleteObject(key);
```

### Image Dimension Extraction
```java
// Use ImageIO to get dimensions after upload
BufferedImage image = ImageIO.read(objectStorageService.getObjectAsInputStream(key));
int width = image.getWidth();
int height = image.getHeight();
```

### Media Type Mapping from MIME
```java
private MediaType getMediaTypeFromMimeType(String mimeType) {
    if (mimeType.startsWith("image/")) return MediaType.IMAGE;
    if (mimeType.startsWith("video/")) return MediaType.VIDEO;
    if (mimeType.startsWith("audio/")) return MediaType.AUDIO;
    return MediaType.DOCUMENT;
}
```
