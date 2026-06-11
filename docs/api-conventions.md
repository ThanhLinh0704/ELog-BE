# ELog — API Conventions
> Load via `@docs/api-conventions.md` when writing API endpoints.
> CLAUDE.md §8 has the base URL — this file adds full conventions.

---

## Base URL

```
/api/v1/{resource}
```

All paths are relative to the server root. Frontend Axios is pre-configured with `baseURL: '/api/v1'`.

---

## Authentication

```http
Authorization: Bearer <jwt-token>
```

- Token obtained from `POST /api/v1/auth/login`
- Token expiry: 24 hours (`elog.jwt.expiration-ms`)
- Refresh: not implemented in INC-1 (re-login required)
- Missing/invalid token → `401 Unauthorized`
- Insufficient role → `403 Forbidden`

---

## Standard Response Envelope

### Success (2xx)
```json
{
  "success": true,
  "data": { ... },           // single object or array
  "message": "Optional info message"
}
```

### Paginated Success
```json
{
  "success": true,
  "data": [ ... ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

### Error (4xx / 5xx)
```json
{
  "success": false,
  "error": {
    "code": "STORE_NOT_ASSIGNABLE",
    "message": "Human-readable message",
    "details": [ "storeCode: ABC not mapped to any route" ]   // optional array
  }
}
```

---

## HTTP Status Code Convention

| Status | When to use |
|--------|-------------|
| 200 OK | Successful GET, PUT |
| 201 Created | Successful POST that creates a resource |
| 204 No Content | Successful DELETE |
| 400 Bad Request | Validation errors (field-level) |
| 401 Unauthorized | Missing or invalid JWT |
| 403 Forbidden | Valid JWT but insufficient role |
| 404 Not Found | Resource does not exist |
| 409 Conflict | State conflict (e.g., fleet capacity exceeded, invalid state transition) |
| 422 Unprocessable | Business rule violation (e.g., unmapped store) |
| 500 Internal | Unexpected server error |

---

## Error Codes (Application-level)

| Code | Trigger |
|------|---------|
| `STORE_NOT_ASSIGNABLE` | Store has no route mapping (BR-02) |
| `CAPACITY_EXCEEDED` | m³ or kg exceeds vehicle limit (BR-03) |
| `FLEET_CAPACITY_INSUFFICIENT` | Total fleet < day load (BR-08) |
| `INVALID_STATE_TRANSITION` | DC-01 violation |
| `EPOD_REQUIRED` | Attempt to complete stop without e-POD (BR-11) |
| `EXCEL_PARSE_ERROR` | Excel file format/content invalid |
| `RESOURCE_NOT_FOUND` | Generic 404 |
| `ACCESS_DENIED` | Role not permitted |

---

## Pagination Convention

Query params on all list endpoints:
```
?page=0&size=20&sort=createdAt,desc
```
- `page` is 0-indexed (Spring Pageable default)
- `size` default: 20, max: 100
- `sort` format: `field,direction` (asc/desc)

---

## Resource Naming Rules

| Pattern | Example |
|---------|---------|
| Noun plural for collections | `/routes`, `/trips`, `/vehicles` |
| Noun singular for sub-resources | `/trips/{id}/stops/{stopId}` |
| Verb for actions (POST only) | `/trips/{id}/dispatch`, `/trips/{id}/validate` |
| Excel import endpoint | `POST /orders/import` (multipart/form-data) |

---

## File Upload Convention (Excel Import)

```http
POST /api/v1/orders/import
Content-Type: multipart/form-data

Form field: file (Excel .xlsx only)
Max size: 10MB
```

Response (success):
```json
{
  "success": true,
  "data": {
    "importId": "uuid",
    "totalRows": 200,
    "validRows": 195,
    "invalidRows": 5,
    "errors": [
      { "row": 3, "column": "storeCode", "reason": "Store not found" }
    ]
  }
}
```

---

## Swagger / OpenAPI

- URL: `http://localhost:8080/swagger-ui/index.html`
- All endpoints must have `@Operation(summary = "...")` and `@ApiResponse` annotations
- Controller-level: `@Tag(name = "Trips", description = "...")`
