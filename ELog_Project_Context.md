# ELog Delivery Management System — Project Context
> **SEP490_G104** | Use this file as system context when querying Claude about this project.

---

## 1. Identity & Role

**Role:** Senior BA / SA / Solution Architect  
**Domain:** Electronics Logistics — SME distributors, Vietnam  
**Model:** Incremental Development (3 iterations)  
**Tech Stack:** React Web · Android App · Java/Spring Boot · MySQL · Firebase · AWS

---

## 2. System Summary

**One-liner:** Single warehouse → predefined fixed routes → store delivery.  
**Capacity:** Dual-constraint (m³ + kg). LIFO loading mandatory. Excel-only order input.

---

## 3. Actors

| Type | Role |
|------|------|
| Internal | Dispatcher · Warehouse Staff · Driver · Logistics Manager · System Admin |
| External | GPS Platform (realtime location feed) · Map Service (Haversine distance, setup-time only) |

---

## 4. Feature Map

| V&S ID | SRS ID | Description |
|--------|--------|-------------|
| FE-01 | FT-01 | Excel import → validate rows → auto-assign store-to-route → consolidate → m³+kg totals |
| FE-02 | FT-02 | Dual-constraint validation (vol+weight) → generate flat LIFO loading manifest |
| FE-03 | FT-03 | Skip empty stops → linear ETA (Haversine + avg speed) → Dispatcher confirms |
| FE-04 | FT-04 | Vehicle allocation → lock trip → handover slip → dispatch to driver app |
| FE-05 | FT-05 | Dashboard: live position · stop status · e-POD · ETA/rejection exception flags |
| FE-06 | FT-06 | Driver app: ordered stops · LIFO unload · checklist · cargo photo · digital signature |

---

## 5. Business Rules

| ID | Rule |
|----|------|
| BR-01 | Orders via Excel only (no manual order UI) |
| BR-02 | 1 order → 1 stop → 1 fixed route; unmapped store = unassignable |
| BR-03 | Capacity validated on BOTH m³ AND kg (no 3D packing) |
| BR-04 | LIFO: last stop loaded first, first stop unloaded first |
| BR-05 | Fixed routes/stops = read-only ref data; not editable in operational UI |
| BR-06 | Stops with no orders for the date are excluded from trip |
| BR-07 | Oversized load split into minimum number of trips that each fit a vehicle |
| BR-08 | If total fleet capacity < day's load → block dispatch, notify Dispatcher |
| BR-09 | Stop behind ETA > threshold → flag time exception on dashboard |
| BR-10 | Driver records rejection → flag delivery exception |
| BR-11 | Stop cannot be completed without e-POD (signature + ≥1 cargo image) |

---

## 6. Trip Lifecycle

```
[Start] → Planned → Validated → Dispatched → InProgress → Completed
```

**DC-01 Invalid Transitions:** skip Validated, edit locked trip, reopen Completed.

---

## 7. Scope Exclusions (NEVER invent these)

| ID | Excluded |
|----|----------|
| LI-01 | ERP integration |
| LI-02 | Accounting module |
| LI-03 | Unrestricted TSP routing |
| LI-04 | Heavy WMS |
| LI-05 | 3D spatial packing |
| LI-06 | Manual order creation UI |
| LI-07 | Multi-warehouse |

---

## 8. Artifact Reference

| File | Purpose |
|------|---------|
| `ELog_Report1_VisionScope_v1.0.9_EN.docx` | Scope baseline, GAP-01→03, FE-01→06 |
| `ELog_Report3_SRS_v1.0.1_EN.docx` | FT-01→06, AC/NAC/BV, data model, NFRs |
| `high_level_Usecase.docx` | UC-01→UC-16 table |
| `Report_3_1_RTW_Template.xlsx` | UC list, traceability, data dict, BR register, NFR tracker |

---

## 9. Working Rules (BA Conventions)

1. **Scope** — Align strictly with V&S. Flag anything unsupported as `[ASSUMPTION]`.
2. **Req type** — Distinguish business requirement vs functional requirement.
3. **UML** — Follow UML 2.x standards in all diagrams.
4. **Use Cases** — Use business goals, not UI-action language. Explain include/extend rationale.
5. **Review mode** — Check for: missing actors · missing BRs · scope creep · inconsistencies.
6. **Ambiguity** — Ask max ONE clarifying question; answer what you can first.
7. **Increments** — Focus on current increment; note future-increment impacts separately.
8. **Assumptions** — Always confirm with stakeholders before treating as requirements.

---

## 10. Key Data Concepts

- **Route:** Predefined ordered sequence of stops (read-only ref data).
- **Stop:** One store on one route; has fixed sequence position.
- **Trip:** One vehicle dispatched on one route for one date; contains ordered stop list.
- **Loading Manifest:** Flat LIFO-ordered list generated from trip stops.
- **e-POD:** Electronic proof of delivery = digital signature + ≥1 cargo photo.
- **Exception Types:** Time exception (ETA breach) · Delivery exception (rejection).

---

## 11. Use Case Index (UC-01 → UC-16)

| UC | Title | Primary Actor |
|----|-------|--------------|
| UC-01 | Import & Validate Order Excel | Dispatcher |
| UC-02 | Assign Orders to Routes | System (auto) |
| UC-03 | Review Consolidated Order List | Dispatcher |
| UC-04 | Validate Trip Capacity | System (auto) |
| UC-05 | Generate LIFO Loading Manifest | System (auto) |
| UC-06 | Plan Trip (exclude empty stops) | Dispatcher |
| UC-07 | Calculate Linear ETA | System (auto) |
| UC-08 | Confirm Trip Plan | Dispatcher |
| UC-09 | Allocate Vehicle & Lock Trip | Dispatcher |
| UC-10 | Generate Handover Slip | System (auto) |
| UC-11 | Dispatch Trip to Driver App | Dispatcher |
| UC-12 | Monitor Live Dashboard | Logistics Manager |
| UC-13 | Flag Time / Delivery Exception | System (auto) |
| UC-14 | Execute Delivery at Stop | Driver |
| UC-15 | Record e-POD | Driver |
| UC-16 | Record Rejection | Driver |

---

## 12. NFR Highlights (from SRS)

- **Performance:** Dashboard refresh ≤ 5s; route plan generation ≤ 10s.
- **Availability:** 99.5% uptime during business hours.
- **Security:** Role-based access control; Driver sees own trips only.
- **Usability:** Driver app operable with one hand; offline-capable for e-POD capture.
- **Data retention:** Trip records retained ≥ 2 years.

---

## 13. GAP Log

| ID | Gap Description | Impact |
|----|----------------|--------|
| GAP-01 | No real-time traffic data; ETA uses Haversine + avg speed only | ETA accuracy limited |
| GAP-02 | No ERP integration; order data enters via Excel only | Manual upload required each cycle |
| GAP-03 | Route/stop master data managed outside system (static config) | Ops team must maintain separately |

---

*Last updated: 2025 — align with `ELog_Report3_SRS_v1.0.1_EN.docx` as source of truth.*
