# ELog — Business Rules Reference
> Load via `@docs/business-rules.md` when coding business logic / validation.
> CLAUDE.md §5 has top-5 summary — this file has full register + edge cases.

---

## Full Business Rule Register (BR-01 → BR-11)

| ID | Rule | Implementation Notes |
|----|------|---------------------|
| BR-01 | Orders via Excel only — no manual order creation UI | Upload API only; reject any POST /orders with body |
| BR-02 | 1 order → 1 stop → 1 fixed route; unmapped store = unassignable | Return 422 with unassignable store list |
| BR-03 | Capacity validated on BOTH m³ AND kg (no 3D packing) | Both must be ≤ vehicle capacity; fail if either exceeds |
| BR-04 | LIFO: last stop on route loaded first, first stop unloaded first | Manifest index = reverse of stop sequence |
| BR-05 | Fixed routes/stops = read-only reference data; not editable in operational UI | No PUT/DELETE on routes or route_stops from non-admin roles |
| BR-06 | Stops with no orders for the date are excluded from trip | Filter at trip planning time; don't include 0-order stops |
| BR-07 | Oversized load → split into minimum number of trips that each fit a vehicle | Greedy bin-packing heuristic (not optimal) |
| BR-08 | If total fleet capacity < day's load → block dispatch + notify Dispatcher | Return 409 with fleet utilization summary |
| BR-09 | Stop behind ETA > threshold → flag time exception on dashboard | Threshold = configurable (default 15 min) |
| BR-10 | Driver records rejection → flag delivery exception | Status: REJECTED; triggers exception record |
| BR-11 | Stop cannot be completed without e-POD (signature + ≥1 cargo image) | Backend validates before accepting COMPLETE status |

---

## Edge Cases — Most Commonly Missed

### BR-02 Edge Cases
- Store exists in master data but has no route assignment → unassignable (not a data error)
- Same store on two routes → invalid master data; flag as `[DATA_ERROR]`, do not auto-pick

### BR-03 Edge Cases
- Volume = 0 but weight > capacity → still fails (weight is the binding constraint)
- Both dimensions exactly at capacity → PASS (≤, not <)

### BR-04 Edge Cases
- If a stop is removed (BR-06), LIFO sequence re-indexes without gap
- Do NOT reverse the full route; reverse only the loaded stops

### BR-07 Edge Cases
- "Minimum number of trips" = ceiling(total_volume / vehicle_volume) AND ceiling(total_weight / vehicle_weight), take max
- Each individual order must fit in a single vehicle (no order splitting)

### BR-11 Edge Cases
- Partial e-POD (signature only, no image) → REJECT completion
- Image uploaded but no signature → REJECT completion

---

## DC-01 — Invalid Trip State Transitions

```
Allowed transitions only:
Planned     → Validated
Validated   → Dispatched
Dispatched  → InProgress
InProgress  → Completed

FORBIDDEN:
Planned     → Dispatched        (skip Validated)
Validated   → InProgress        (skip Dispatched)
Any state   → Planned           (reopen — not allowed)
Completed   → ANY               (terminal state)
Dispatched  → Validated         (cannot go back)
```

---

## GAP Log — Implementation Impact

| ID | Gap | What to do in code |
|----|-----|--------------------|
| GAP-01 | No real-time traffic; ETA = Haversine + avg speed | Use formula: distance / avg_speed_kmh → add at each stop |
| GAP-02 | No ERP; orders enter via Excel only | ExcelParser is the only order creation path |
| GAP-03 | Route/stop master data managed outside system (static) | Provide admin import for routes; no dynamic editing |

---

## Actors — Role & Permission Matrix

| Role | Key Permissions |
|------|----------------|
| Dispatcher | Import Excel · Plan trips · Confirm/dispatch · Monitor dashboard |
| Warehouse Staff | View loading manifest · Confirm loading |
| Driver | View own trips · Record e-POD · Record rejection |
| Logistics Manager | Read-only dashboard · View KPIs |
| System Admin | User management · Master data management |
| GPS Platform | Push location updates (service-to-service) |
| Map Service | Called once at setup for route distance data |

---

## Use Case → Business Rule Traceability

| UC | Business Rules Applied |
|----|----------------------|
| UC-01 Import Excel | BR-01 |
| UC-02 Assign to Routes | BR-02 |
| UC-04 Validate Capacity | BR-03, BR-07, BR-08 |
| UC-05 LIFO Manifest | BR-04 |
| UC-06 Plan Trip | BR-06 |
| UC-09 Lock Trip | DC-01 (Validated→Dispatched) |
| UC-13 Flag Exception | BR-09, BR-10 |
| UC-14/15 Execute Delivery | BR-11 |
| UC-16 Record Rejection | BR-10 |
