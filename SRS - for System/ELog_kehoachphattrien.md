# ELog Delivery Management System

## Incremental Development Plan

## Project Context

- Domain: Electronics Logistics
- Team: 5 members
- Sprint Length: 2 weeks
- Tech Stack:
  - Spring Boot
  - ReactJS
  - MySQL
  - JWT
  - REST API

## Scope

### Included

- Web Dashboard
- Dispatcher
- Warehouse Staff
- Driver (account only)
- System Admin

### Deferred

- Android Driver App
- e-POD
- GPS Realtime Tracking

---

# INC-1 Foundation & Master Data

## EP-01 Project Setup & Infrastructure

### US-01 Project Skeleton

**Goal**
Developer needs Backend + Frontend + Database skeleton ready.

**Tasks**

- Create initial DB schema & Flyway migration
- Create Spring Boot project structure
- Create ReactJS project structure
- Setup Git strategy & PR template
- Setup CI pipeline
- Write environment setup guide

---

## EP-02 Authentication & Authorization

### US-02 JWT Authentication

**Goal**
Users can login securely with role-based access.

**Tasks**

- Design User / Role tables
- Implement JWT Authentication
- Implement RBAC authorization
- Create Login UI
- Authentication testing
- API documentation

### US-03 User Management

**Goal**
Admin manages users and roles.

**Tasks**

- User CRUD API
- User Management UI
- Authorization testing

---

## EP-03 Master Data

### US-04 Route Management

**Goal**
Manage predefined routes and ordered stops.

**Tasks**

- Design Route / RouteStop schema
- Route CRUD API
- Route Management UI
- Validation testing

### US-05 Store Management

**Goal**
Manage registered stores.

**Tasks**

- Store CRUD API
- Store Management UI
- Validation testing

### US-06 Vehicle Management

**Goal**
Manage vehicles and capacities.

**Tasks**

- Vehicle schema
- Vehicle CRUD API
- Vehicle Management UI
- Validation testing

### US-07 Product Management

**Goal**
Manage products and dimensions.

**Tasks**

- Product schema
- Product CRUD API
- Auto volume calculation
- Product Management UI
- Validation testing
- Data Dictionary

---

# INC-2 Order Import & Route Consolidation

## EP-04 Excel Import & Route Grouping

### US-08 Excel Import

**Goal**
Dispatcher imports daily orders from Excel.

**Tasks**

- Order schema
- Excel Parser
- Upload API
- Import UI
- Parser testing

### US-09 Import Validation

**Goal**
Show invalid rows during import.

**Tasks**

- Validation rules
- Error API
- Error UI
- Validation testing

### US-10 Route Consolidation

**Goal**
Automatically group orders by route and calculate volume.

**Tasks**

- Route grouping service
- Aggregation API
- Consolidation UI
- Calculation testing
- API documentation

---

# INC-3 Capacity Planning, LIFO & ETA

## EP-05 Capacity Validation & LIFO

### US-11 Capacity Validation

**Goal**
Check whether assigned vehicle can carry route volume.

**Tasks**

- Capacity validation service
- Capacity API
- Capacity UI
- Boundary testing

### US-12 LIFO Manifest

**Goal**
Generate loading sequence based on delivery order.

**Tasks**

- Manifest schema
- LIFO algorithm
- Manifest API
- Manifest UI
- LIFO testing

---

## EP-06 Route Planning & ETA

### US-13 Stop Filtering

**Goal**
Skip stops without orders.

**Tasks**

- Active stop filtering service
- Route API
- Route UI
- Testing

### US-14 ETA Calculation

**Goal**
Calculate ETA for each stop.

**Tasks**

- ETA algorithm
- ETA API
- ETA UI
- ETA testing
- Technical documentation

---

# INC-4 Vehicle Assignment & Dispatching

## EP-07 Dispatch Planning

### US-15 Vehicle Assignment

**Goal**
Assign suitable vehicle to route.

**Tasks**

- Trip schema
- Vehicle recommendation service
- Assignment API
- Assignment UI
- Validation testing

### US-16 Dispatch Execution

**Goal**
Lock trip and generate dispatch documents.

**Tasks**

- Trip state model
- State machine
- Dispatch API
- Dispatch UI
- State transition testing
- API documentation

---

# INC-5 Monitoring & KPI

## EP-08 Trip Monitoring

### US-17 Dashboard Monitoring

**Goal**
Monitor ongoing trips.

**Tasks**

- Progress schema
- Monitoring service
- Dashboard API
- Dashboard UI
- Testing

### US-18 Exception Management

**Goal**
Record operational exceptions.

**Tasks**

- Exception API
- Exception UI
- Testing

---

## EP-09 KPI Reporting

### US-19 KPI Dashboard

**Goal**
View operational KPIs.

**Tasks**

- KPI queries
- KPI calculation service
- KPI API
- KPI Dashboard UI
- KPI testing
- User documentation

---

# Sprint Plan

## Sprint 1

- US-01
- US-02
- US-03
- US-04

Deliverables:

- Project Skeleton
- Authentication
- User Management
- Route Management

## Sprint 2

- US-05
- US-06
- US-07
- US-08

Deliverables:

- Master Data Complete
- Excel Import

## Sprint 3

- US-09
- US-10

Deliverables:

- Import Validation
- Route Consolidation

## Sprint 4

- US-11
- US-12

Deliverables:

- Capacity Check
- LIFO Manifest

## Sprint 5

- US-13
- US-14
- US-15

Deliverables:

- Route Planning
- ETA
- Vehicle Assignment

## Sprint 6

- US-16

Deliverables:

- Dispatch Management

## Sprint 7

- US-17
- US-18
- US-19

Deliverables:

- Monitoring Dashboard
- Exception Management
- KPI Reporting
