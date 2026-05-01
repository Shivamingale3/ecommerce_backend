# Phase 0: Admin Authentication & Profile

**Database Tables:** AdminSignInRequest, Admin, AdminRepository, AdminSignInRequestRepository
**Existing Services:** AdminAuthService, AdminJwtTokenProvider

---

## 0.1 Admin Login (OTP Request)
**Endpoint:** `POST /api/admin/v1/auth/login`
**Security:** Public (no auth required)

### Request
```json
{
  "email": "admin@example.com"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| email | String | Yes | Valid email format, must exist in Admin table |

### Process
1. Validate email format and existence in DB
2. Check Admin.enabled == true
3. Generate 6-digit OTP
4. Delete any existing AdminSignInRequest for this email
5. Save new AdminSignInRequest with OTP and validTill (now + 5 minutes)
6. Send OTP email via EmailTemplateService
7. Return requestId (AdminSignInRequest.id) and validTill

### DB Actions
- **Insert:** AdminSignInRequest (email, otp, validTill)
- **Delete:** AdminSignInRequest WHERE email = :email AND deleted = false (cleanup old)

### Response (200)
```json
{
  "success": true,
  "data": {
    "requestId": "01ARZ3NDE...",
    "validTill": "2026-05-01T10:05:00Z",
    "message": "OTP sent to admin@example.com"
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Admin not found | Email not in Admin table |
| FORBIDDEN | Admin account is disabled | Admin.enabled = false |
| INTERNAL_SERVER_ERROR | Failed to send OTP email | Email service failure |

---

## 0.2 Verify OTP
**Endpoint:** `POST /api/admin/v1/auth/verify-otp`
**Security:** Public (no auth required)

### Request
```json
{
  "requestId": "01ARZ3NDE...",
  "otp": "123456"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| requestId | String | Yes | Must exist and not deleted |
| otp | String | Yes | Must match, exactly 6 digits |

### Process
1. Find AdminSignInRequest by requestId
2. Check validTill > now (not expired)
3. Check otp matches exactly
4. Find Admin by email
5. Check Admin.enabled == true
6. Delete AdminSignInRequest (one-time use)
7. Generate JWT access token and refresh token
8. Return tokens

### DB Actions
- **Select:** AdminSignInRequest WHERE id = :requestId
- **Select:** Admin WHERE email = :email AND deleted = false
- **Soft Delete:** AdminSignInRequest (set deleted = true)

### Response (200)
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "expiresIn": 3600
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Invalid request ID | AdminSignInRequest not found |
| BAD_REQUEST | OTP has expired | validTill < now |
| FORBIDDEN | Invalid OTP | otp mismatch |
| FORBIDDEN | Admin account is disabled | Admin.enabled = false |

---

## 0.3 Resend OTP
**Endpoint:** `POST /api/admin/v1/auth/resend-otp`
**Security:** Public (no auth required)

### Request
```json
{
  "requestId": "01ARZ3NDE...",
  "email": "admin@example.com"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| requestId | String | Yes | Must exist |
| email | String | Yes | Must match original request |

### Process
1. Find AdminSignInRequest by requestId
2. Verify email matches
3. Delete old request
4. Generate new OTP
5. Save new AdminSignInRequest
6. Send OTP email
7. Return new requestId and validTill

### DB Actions
- **Select:** AdminSignInRequest WHERE id = :requestId
- **Delete:** AdminSignInRequest WHERE id = :requestId
- **Insert:** AdminSignInRequest (new OTP)

### Response (200)
```json
{
  "success": true,
  "data": {
    "requestId": "01ARZ4NDE...",
    "validTill": "2026-05-01T10:10:00Z",
    "message": "New OTP sent"
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Invalid request ID | AdminSignInRequest not found |
| BAD_REQUEST | Email mismatch | email != original email |

---

## 0.4 Refresh Token
**Endpoint:** `POST /api/admin/v1/auth/refresh`
**Security:** Public (no auth required)

### Request
```json
{
  "refreshToken": "eyJhbGc..."
}
```

### Process
1. Validate refresh token
2. Extract adminId from token
3. Find Admin
4. Generate new access token
5. Optionally generate new refresh token (token rotation) or keep same
6. Return new tokens

### DB Actions
- **Select:** Admin WHERE id = :adminId AND deleted = false

### Response (200)
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "expiresIn": 3600
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| UNAUTHORIZED | Invalid or expired refresh token | Token invalid/expired |
| FORBIDDEN | Admin account is disabled | Admin.enabled = false |

---

## 0.5 Get Admin Profile
**Endpoint:** `GET /api/admin/v1/profile`
**Security:** Required (JWT Bearer token, type="admin")

### Process
1. Extract adminId from JWT
2. Find Admin
3. Return profile data

### DB Actions
- **Select:** Admin WHERE id = :adminId AND deleted = false

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "name": "Admin User",
    "email": "admin@example.com",
    "mobile": "+1234567890",
    "enabled": true,
    "createdAt": "2026-01-01T00:00:00Z"
  }
}
```

### Errors
| Code | Message | Condition |
|------|---------|-----------|
| NOT_FOUND | Admin not found | Admin deleted or not found |

---

## 0.6 Update Admin Profile
**Endpoint:** `PUT /api/admin/v1/profile`
**Security:** Required (JWT Bearer token, type="admin")

### Request
```json
{
  "name": "Admin User Updated",
  "mobile": "+1987654321"
}
```

### Validation
| Field | Type | Required | Rules |
|-------|------|----------|-------|
| name | String | No | Max 100 chars |
| mobile | String | No | Max 20 chars |

### Process
1. Extract adminId from JWT
2. Find Admin
3. Update only provided fields
4. Save
5. Return updated profile

### DB Actions
- **Update:** Admin (name, mobile) WHERE id = :adminId

### Response (200)
```json
{
  "success": true,
  "data": {
    "id": "01ARZ3NDE...",
    "name": "Admin User Updated",
    "email": "admin@example.com",
    "mobile": "+1987654321",
    "enabled": true,
    "updatedAt": "2026-05-01T10:00:00Z"
  },
  "message": "Profile updated successfully"
}
```

---

## 0.7 Logout
**Endpoint:** `POST /api/admin/v1/auth/logout`
**Security:** Required (JWT Bearer token, type="admin")

### Process
1. Extract adminId from JWT
2. (Optional) Add token to blacklist in Redis
3. Client discards tokens

### Response (200)
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

## Notes for Junior Engineers

### Security Points
- OTP is 6 digits: `String.format("%06d", (int)(Math.random() * 900000) + 100000)`
- Tokens stored in HTTP-only cookies (existing implementation)
- All admin endpoints require `type="admin"` claim in JWT
- Token expiry: access=1 hour, refresh=7 days (check existing config)

### Existing Code to Reuse
- `AdminAuthService` (already has requestSignInOtp, verifySignInOtp, resendSignInOtp)
- `AdminJwtTokenProvider` (generateAccessToken, generateRefreshToken, validateToken)
- `AdminRepository` (extend BaseRepository with deleted filter)
- `EmailTemplateService.sendOtpEmail()`
