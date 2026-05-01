# Admin Panel Task Tracker

## How to Use
Status values:
- `[ ]` = Not Started
- `[o]` = In Progress
- `[x]` = Completed

Update the checkbox in the file when you complete a task.

---

## Phase 0: Admin Authentication & Profile

| # | Task | Endpoint | Status | Notes |
|---|------|----------|--------|-------|
| 0.1 | Admin Login (OTP Request) | POST /api/admin/v1/auth/login | [ ] | |
| 0.2 | Verify OTP | POST /api/admin/v1/auth/verify-otp | [ ] | Existing in AdminAuthService |
| 0.3 | Resend OTP | POST /api/admin/v1/auth/resend-otp | [ ] | Existing in AdminAuthService |
| 0.4 | Refresh Token | POST /api/admin/v1/auth/refresh | [ ] | |
| 0.5 | Get Admin Profile | GET /api/admin/v1/profile | [ ] | |
| 0.6 | Update Admin Profile | PUT /api/admin/v1/profile | [ ] | |
| 0.7 | Logout | POST /api/admin/v1/auth/logout | [ ] | |

---

## Phase 1: Media Library

| # | Task | Endpoint | Status | Notes |
|---|------|----------|--------|-------|
| 1.1 | Request Upload URL | POST /api/admin/v1/media/upload-url | [ ] | |
| 1.2 | Confirm Upload | POST /api/admin/v1/media/confirm-upload | [ ] | |
| 1.3 | List Media | GET /api/admin/v1/media | [ ] | |
| 1.4 | Get Single Media | GET /api/admin/v1/media/{id} | [ ] | |
| 1.5 | Update Media | PUT /api/admin/v1/media/{id} | [ ] | |
| 1.6 | Delete Media | DELETE /api/admin/v1/media/{id} | [ ] | |
| 1.7 | Get/Refresh Presigned URL | GET /api/admin/v1/media/{id}/url | [ ] | |
| 1.8 | Pending Upload Cleanup | Scheduled Task | [ ] | Redis TTL |
| 1.9 | Get Unassigned Media | GET /api/admin/v1/media/unassigned | [ ] | Phase 7 |

---

## Phase 2: Category Management

| # | Task | Endpoint | Status | Notes |
|---|------|----------|--------|-------|
| 2.1 | Create Category | POST /api/admin/v1/categories | [ ] | |
| 2.2 | List Categories | GET /api/admin/v1/categories | [ ] | |
| 2.3 | Get Single Category | GET /api/admin/v1/categories/{id} | [ ] | |
| 2.4 | Update Category | PUT /api/admin/v1/categories/{id} | [ ] | |
| 2.5 | Reorder Categories | PUT /api/admin/v1/categories/reorder | [ ] | |
| 2.6 | Delete Category | DELETE /api/admin/v1/categories/{id} | [ ] | |
| 2.7 | Set Category Image | PUT /api/admin/v1/categories/{id}/image | [ ] | |

---

## Phase 3: Brand Management

| # | Task | Endpoint | Status | Notes |
|---|------|----------|--------|-------|
| 3.1 | Create Brand | POST /api/admin/v1/brands | [ ] | |
| 3.2 | List Brands | GET /api/admin/v1/brands | [ ] | |
| 3.3 | Get Single Brand | GET /api/admin/v1/brands/{id} | [ ] | |
| 3.4 | Update Brand | PUT /api/admin/v1/brands/{id} | [ ] | |
| 3.5 | Delete Brand | DELETE /api/admin/v1/brands/{id} | [ ] | |

---

## Phase 4: Tag Management

| # | Task | Endpoint | Status | Notes |
|---|------|----------|--------|-------|
| 4.1 | Create Tag | POST /api/admin/v1/tags | [ ] | |
| 4.2 | Create Tag Inline | POST /api/admin/v1/tags/inline | [ ] | |
| 4.3 | List Tags | GET /api/admin/v1/tags | [ ] | |
| 4.4 | Update Tag | PUT /api/admin/v1/tags/{id} | [ ] | |
| 4.5 | Delete Tag | DELETE /api/admin/v1/tags/{id} | [ ] | |

---

## Phase 5: Product Management

| # | Task | Endpoint | Status | Notes |
|---|------|----------|--------|-------|
| 5.1 | Create Product | POST /api/admin/v1/products | [ ] | Auto-create inventory |
| 5.2 | List Products | GET /api/admin/v1/products | [ ] | |
| 5.3 | Get Product Detail | GET /api/admin/v1/products/{id} | [ ] | |
| 5.4 | Update Product Basic | PUT /api/admin/v1/products/{id} | [ ] | |
| 5.5 | Assign Categories/Tags | PUT /api/admin/v1/products/{id}/relations | [ ] | |
| 5.6 | Assign Media | PUT /api/admin/v1/products/{id}/media | [ ] | |
| 5.7 | Set Pricing | PUT /api/admin/v1/products/{id}/pricing | [ ] | |
| 5.8 | Update Inventory | PUT /api/admin/v1/products/{id}/inventory | [ ] | |
| 5.9 | Set Shipping/Dimensions | PUT /api/admin/v1/products/{id}/shipping | [ ] | |
| 5.10 | Set SEO Fields | PUT /api/admin/v1/products/{id}/seo | [ ] | |
| 5.11 | Set Featured/Visibility | PUT /api/admin/v1/products/{id}/visibility | [ ] | |
| 5.12 | Change Status | PUT /api/admin/v1/products/{id}/status | [ ] | |
| 5.13 | Delete Product | DELETE /api/admin/v1/products/{id} | [ ] | |
| 5.14 | Bulk Status Change | PUT /api/admin/v1/products/bulk/status | [ ] | |
| 5.15 | Bulk Delete | DELETE /api/admin/v1/products/bulk | [ ] | |
| 5.16 | Get Product Media | GET /api/admin/v1/products/{id}/media | [ ] | Phase 7 |

---

## Phase 6: Inventory Management

| # | Task | Endpoint | Status | Notes |
|---|------|----------|--------|-------|
| 6.1 | List Low Stock | GET /api/admin/v1/inventory/low-stock | [ ] | |
| 6.2 | Bulk Stock Update | PUT /api/admin/v1/inventory/bulk | [ ] | |
| 6.3 | Stock Adjustment | POST /api/admin/v1/inventory/{productId}/adjust | [ ] | |
| 6.4 | Reserve Stock | POST /api/admin/v1/inventory/{productId}/reserve | [ ] | Future (Order module) |

---

## Progress Dashboard

Open [index.html](index.html) in a browser for a visual dashboard with:
- Phase-by-phase progress tracking
- Search and filter by status (pending/in-progress/completed)
- Auto-saves to browser localStorage
- Import/export data as JSON
- Link to phase documentation

---

## Progress Summary

| Phase | Total Tasks | Completed | In Progress |
|-------|-------------|-----------|-------------|
| Phase 0 | 7 | 0 | 0 |
| Phase 1 | 9 | 0 | 0 |
| Phase 2 | 7 | 0 | 0 |
| Phase 3 | 5 | 0 | 0 |
| Phase 4 | 5 | 0 | 0 |
| Phase 5 | 16 | 0 | 0 |
| Phase 6 | 4 | 0 | 0 |
| **Total** | **53** | **0** | **0** |

*Use the dashboard to track progress visually. Data persists in browser localStorage.*

---

## Progress Dashboard

Open [index.html](index.html) in a browser for a visual dashboard with:
- Phase-by-phase progress tracking
- Search and filter by status
- Auto-saves to localStorage
- Import/export data

---

## File Structure

```
docs/admin-panel/
├── index.html               # Visual task dashboard (open in browser)
├── TASK_TRACKER.md          # This file
├── task-outline.md          # High-level overview
├── tasks.json               # Machine-readable task list
├── PHASE_0_Auth_Profile.md  # Auth & profile details
├── PHASE_1_Media_Library.md # Media upload flow
├── PHASE_2_Category_Mgmt.md # Category CRUD
├── PHASE_3_Brand_Mgmt.md   # Brand CRUD
├── PHASE_4_Tag_Mgmt.md     # Tag CRUD
├── PHASE_5_Product_Mgmt.md # Product CRUD
├── PHASE_6_Inventory_Mgmt.md # Inventory management
└── PHASE_7_Media_Assign.md  # Media assignment views
```