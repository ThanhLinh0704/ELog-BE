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
| External | GPS Platform (realtime location feed, fed by Driver app) · Map Service (Haversine distance, setup-time only — accessed by System Admin during Store Master Data configuration) |

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

| ID | Rule | Covered by UC |
|----|------|----------------|
| BR-01 | Orders via Excel only (no manual order UI) | UC-05 |
| BR-02 | 1 order → 1 stop → 1 fixed route; unmapped store = unassignable | UC-05 |
| BR-03 | Capacity validated on BOTH m³ AND kg (no 3D packing) | UC-07 |
| BR-04 | LIFO: last stop loaded first, first stop unloaded first | UC-07, UC-09, UC-10, UC-15 |
| BR-05 | Fixed routes/stops = read-only ref data; not editable in operational UI | UC-03 |
| BR-06 | Stops with no orders for the date are excluded from trip | UC-06, UC-11 |
| BR-07 | Oversized load split into minimum number of trips that each fit a vehicle | UC-18 |
| BR-08 | If total fleet capacity < day's load → block dispatch, notify Dispatcher | UC-17 |
| BR-09 | Stop behind ETA > threshold → flag time exception on dashboard | UC-19 |
| BR-10 | Driver records rejection → flag delivery exception | UC-14 |
| BR-11 | Stop cannot be completed without e-POD (signature + ≥1 cargo image) | UC-16 |

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

## 8. Working Rules (BA Conventions)

1. **Scope** — Align strictly with V&S. Flag anything unsupported as `[ASSUMPTION]`.
2. **Req type** — Distinguish business requirement vs functional requirement.
3. **UML** — Follow UML 2.x standards in all diagrams.
4. **Use Cases** — Use business goals, not UI-action language. Explain include/extend rationale.
5. **Review mode** — Check for: missing actors · missing BRs · scope creep · inconsistencies.
6. **Ambiguity** — Ask max ONE clarifying question; answer what you can first.
7. **Increments** — Focus on current increment; note future-increment impacts separately.
8. **Assumptions** — Always confirm with stakeholders before treating as requirements.

---

## 9. Key Data Concepts

- **Route:** Predefined ordered sequence of stops (read-only ref data).
- **Stop:** One store on one route; has fixed sequence position.
- **Trip:** One vehicle dispatched on one route for one date; contains ordered stop list.
- **Loading Manifest:** Flat LIFO-ordered list generated from trip stops.
- **e-POD:** Electronic proof of delivery = digital signature + ≥1 cargo photo.
- **Exception Types:** Time exception (ETA breach, system-detected, UC-19) · Delivery exception (rejection, driver-recorded, UC-14).

---

## 10. Use Case Index (UC-01 → UC-20) — v2.0 Official Baseline

> Supersedes the previous UC-01→16 index. Renumbered/regrouped by primary actor; added UC-17→UC-20 to cover BR-07, BR-08, BR-09 and the Driver→GPS Platform data flow. Full spec with Pre/Post-conditions: `ELog_UseCase_Specification_v2_0.md`.

| UC | Title | Primary Actor | Relationship |
|----|-------|--------------|---------------|
| UC-01 | Manage User Accounts & Roles | System Admin | — |
| UC-02 | Manage Vehicle Data | System Admin | — |
| UC-03 | Manage Fixed Routes | System Admin | — |
| UC-04 | Manage Store Branches | System Admin | assoc. Map Service (setup-time) |
| UC-05 | Import Orders | Dispatcher | — |
| UC-06 | Confirm Route Plan | Dispatcher | — |
| UC-07 | Loading Capacity Confirmation | Dispatcher | — |
| UC-08 | View KPI Dashboard | Logistics Manager | — |
| UC-09 | View LIFO Instructions | Warehouse Staff | included by UC-10 |
| UC-10 | Load Vehicle & Confirm Completion | Warehouse Staff | `<<include>>` UC-09 |
| UC-11 | Delivery Point Planning & ETA | Dispatcher | — |
| UC-12 | Vehicle Assignment & Trip Coordination | Dispatcher | extended by UC-17, UC-18 |
| UC-13 | Trip Progress Monitoring | Logistics Manager | assoc. GPS Platform; extended by UC-19 |
| UC-14 | Record Delivery Rejection | Driver | extends UC-16 |
| UC-15 | View Trip Details & LIFO Unloading | Driver | includes UC-20 |
| UC-16 | Update Milestones & Submit e-POD | Driver | extended by UC-14 |
| UC-17 | Block Dispatch on Capacity Shortfall | Dispatcher | `<<extend>>` UC-12 |
| UC-18 | Split Oversized Load into Multiple Trips | Dispatcher | `<<extend>>` UC-12 |
| UC-19 | Flag Time Exception | *(System — auto)* | `<<extend>>` UC-13 |
| UC-20 | Broadcast Vehicle Location | Driver | assoc. GPS Platform; included by UC-15 |

**Key changes from v1 (UC-01→16):**
- UC-14 narrowed & renamed (was "Execute Delivery at Stop" combined with exception flagging) → now isolates driver-triggered rejection only (BR-10); system-triggered time exception moved to UC-19.
- UC-15 Map Service association removed (was incorrectly attached to Driver's runtime unloading view; Map Service is setup-time only, now correctly under UC-04).
- UC-17, UC-18, UC-19, UC-20 added — previously BR-07, BR-08, BR-09 had no UC coverage, and the Driver→GPS Platform broadcast direction was missing from the diagram.

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


