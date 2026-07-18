# AT-US10 — L3 System & API Testing (Route Consolidation)

**Mô tả:** Test qua HTTP thật (full stack), theo kỹ thuật Input Domain Partitioning cho từng endpoint — đúng cấu trúc Report 5.3.

**Sprint:** Sprint 3  
**Tester:** [Tester Name]  
**Execution date:** [Execution Date]  
**Environment:** Local — `http://localhost:8080`  
**Tool:** Postman  
**Test level:** L3 System & API Testing  

---

## 1. L3-RouteConsolidation API Cases

| Test ID | Coverage Technique | SRS Ref | Priority | HTTP Method + Endpoint | Auth | Request Body / Params | Expected HTTP | Expected Error Code | Negative? | Actual | Result |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **L3-CON-01** | Input-Domain-Happy | AC-10-1 | P1 | POST /api/trip-drafts/consolidate | Bearer JWT (DISPATCHER) | `{"deliveryDate": "2026-03-16"}` | 200 | None | No | | NOT RUN |
| **L3-CON-02** | Input-Domain-Error | NAC-10b | P1 | POST /api/trip-drafts/consolidate | Bearer JWT (WAREHOUSE_STAFF) | Cùng request | 403 | ACCESS_DENIED | Yes | | NOT RUN |
| **L3-CON-03** | Input-Domain-Error | NAC-10b | P1 | POST /api/trip-drafts/consolidate | Không có token | — | 401 | AUTH_001 | Yes | | NOT RUN |
| **L3-CON-04** | Input-Domain-Error | — | P1 | POST /api/trip-drafts/consolidate | DISPATCHER | `{"deliveryDate": null}` | 400 | BAD_REQUEST (deliveryDate is required) | Yes | | NOT RUN |
| **L3-CON-05** | Input-Domain-Error | — | P2 | POST /api/trip-drafts/consolidate | DISPATCHER | `{"deliveryDate": "2026/03/16"}` (sai định dạng YYYY-MM-DD) | 400 | BAD_REQUEST (JSON parse error) | Yes | | NOT RUN |
| **L3-CON-06** | Input-Domain-Happy | AC-10-2 | P1 | GET /api/trip-drafts | DISPATCHER / WAREHOUSE | Params: `?deliveryDate=2026-03-16` | 200 | None | No | | NOT RUN |
| **L3-CON-07** | Input-Domain-Happy | AC-10-2 | P1 | GET /api/trip-drafts/{id} | DISPATCHER / WAREHOUSE | id = 1 (hoặc id hợp lệ) | 200 | None | No | | NOT RUN |
| **L3-CON-08** | Input-Domain-Error | — | P2 | GET /api/trip-drafts/{id} | DISPATCHER | id = 9999 (không tồn tại) | 404 | RESOURCE_NOT_FOUND | Yes | | NOT RUN |

### Evidence cho L3-RouteConsolidation API
*(Dán ảnh chụp màn hình Postman của bạn vào đây sau khi chạy từng case)*

---

## 2. Acceptance Criteria (Tiêu chí nghiệm thu)

- [ ] Tất cả test case L3-RouteConsolidation pass trên môi trường local (đầy đủ các status code 200, 400, 401, 403, 404).
- [ ] Dữ liệu trả về từ API `GET /api/trip-drafts/{id}` hiển thị đúng thông số Gom tuyến (`totalVolumeM3`, `totalWeightKg`, `activeStopCount`, `skippedStopCount`).
