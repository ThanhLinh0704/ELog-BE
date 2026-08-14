# ELog System Function & Code Map

> Mục tiêu của file này: giúp một người mới vào dự án hiểu hệ thống ELog đang làm gì, từng màn hình dùng để làm gì, và code nào tương ứng với từng chức năng.
>
> Cách đọc nhanh: nếu muốn hiểu nghiệp vụ, đọc từ phần 1 đến phần 4. Nếu muốn sửa code, đọc phần 5 trở đi và tra theo màn hình/chức năng.

---

## 1. Cốt lõi hệ thống

Cốt lõi mà ELog đang hướng tới là **đề xuất xe phù hợp cho chuyến giao hàng**.

Nói đầy đủ hơn:

```text
ELog nhận đơn hàng từ Excel
→ kiểm tra sản phẩm/cửa hàng/tuyến cố định
→ gom đơn thành Trip Draft
→ tính tổng khối lượng kg và thể tích m³
→ kiểm tra xe nào đủ điều kiện
→ đề xuất/gán xe và tài xế
→ dispatch chuyến
→ theo dõi giao hàng, ngoại lệ, kết quả, KPI
```

Vì vậy các màn hình không tồn tại rời rạc. Chúng chia thành 4 nhóm:

1. **Dữ liệu nền**: user, role, driver, store, vehicle, product, route.
2. **Lập chuyến và đề xuất xe**: import Excel, trip draft, capacity, recommendation, assignment, manifest, dispatch.
3. **Thực thi chuyến**: driver nhận chuyến, start, arrive, giao hàng, complete.
4. **Giám sát và báo cáo**: monitoring, exception, trip outcome, KPI, activity history.

---

## 2. Kiến trúc code tổng quan

Workspace chính:

```text
D:\Elog
├── ELog-BE       # Backend Spring Boot
├── ELog-FE       # Frontend React/Vite/Ant Design
└── Elog-Mobile   # Mobile Flutter cho Driver
```

### 2.1 Backend

Repo:

```text
D:\Elog\ELog-BE
```

Các lớp chính:

```text
src/main/java/com/elog/controller   # REST API endpoints
src/main/java/com/elog/service      # Interface nghiệp vụ
src/main/java/com/elog/service/impl # Logic nghiệp vụ thật
src/main/java/com/elog/entity       # Entity/JPA model
src/main/java/com/elog/repository   # Data access
src/main/java/com/elog/dto          # Request/response DTO
src/main/resources/db/migration     # Flyway database migrations
```

Luồng backend chuẩn:

```text
Frontend API call
→ Controller
→ Service interface
→ ServiceImpl
→ Repository
→ Entity/Database
→ DTO response
```

Ví dụ khi Dispatcher gán xe:

```text
FE VehicleAssignmentPage
→ FE tripApi
→ BE TripController
→ TripServiceImpl / RecommendationServiceImpl / CapacityValidationServiceImpl
→ TripDraft, Vehicle, Driver, Trip entities
```

### 2.2 Frontend

Repo:

```text
D:\Elog\ELog-FE
```

Các lớp chính:

```text
src/App.tsx                 # Khai báo route màn hình
src/components/AdminShell.tsx # Layout sidebar/header chính
src/pages                  # Các màn hình
src/api                    # Hàm gọi backend API
src/types                  # TypeScript types
src/utils                  # Mapper, permission, validate, normalize
src/guards                 # Guard theo role/permission
src/components/auth        # Permission route wrapper
```

Luồng frontend chuẩn:

```text
User mở route
→ App.tsx chọn Page component
→ ProtectedRoute/ProtectedPermissionRoute kiểm quyền
→ Page gọi file src/api/*.ts
→ axiosInstance gửi request tới BE
→ Page render table/form/modal/result
```

### 2.3 Mobile

Repo:

```text
D:\Elog\Elog-Mobile
```

Vai trò hiện tại:

- Phục vụ Driver.
- Tập trung vào luồng "Chuyến của tôi".
- Test chuẩn bị ở `integration_test/report5_l4_mobile_test.dart`.

---

## 3. Business rules cần nhớ

Các rule này quyết định logic đề xuất xe và lập chuyến:

| ID | Rule | Ý nghĩa với hệ thống |
|---|---|---|
| BR-01 | Đơn hàng chỉ nhập qua Excel | Không tạo order thủ công |
| BR-02 | Mỗi order giao tới store; store phải thuộc route cố định | Store không có route thì không lập chuyến được |
| BR-03 | Kiểm tra cả kg và m³ | Xe phải đủ cả tải trọng và thể tích |
| BR-04 | LIFO loading | Điểm giao cuối xếp lên xe trước |
| BR-06 | Stop không có order thì loại khỏi chuyến | Trip draft chỉ chứa stop có hàng |
| BR-07 | Quá tải thì chia chuyến tối thiểu | Liên quan split recommendation |
| BR-08 | Fleet không đủ thì block dispatch | Không được chốt chuyến nếu đội xe không đủ |
| BR-09 | Trễ ETA thì flag exception | Dùng ở monitoring/exception |
| BR-10 | Driver reject thì tạo delivery exception | Dùng ở driver flow |
| BR-11 | Complete stop cần e-POD | Dùng ở execution/outcome |

State chuyến:

```text
Planned → Validated → Dispatched → InProgress → Completed
```

Các transition bị cấm:

```text
Planned → Dispatched
Validated → InProgress
Completed → quay lại sửa
Dispatched → Validated
```

---

## 4. Luồng nghiệp vụ chính theo góc nhìn đề xuất xe

### 4.1 Dữ liệu đầu vào

Để đề xuất xe đúng, hệ thống cần dữ liệu sau:

```text
Product  → biết mỗi sản phẩm nặng bao nhiêu, chiếm bao nhiêu m³
Store    → biết đơn giao tới cửa hàng nào
Route    → biết cửa hàng nằm trên tuyến nào
Vehicle  → biết xe nào chở được bao nhiêu kg/m³
Driver   → biết tài xế nào available
Order    → biết hôm nay cần giao gì
```

### 4.2 Bộ xử lý lõi

```text
ImportService
→ TripDraftService
→ CapacityValidationService
→ RecommendationService
→ TripService assignment
```

### 4.3 Đầu ra

Đầu ra quan trọng nhất:

```text
Danh sách xe/phương án xe phù hợp cho Trip Draft
```

Sau đó Dispatcher chọn hoặc xác nhận phương án:

```text
Vehicle recommendation
→ assignment
→ dispatch
→ monitoring
```

---

## 5. Bản đồ màn hình và code tương ứng

### 5.1 `/login` — Đăng nhập

**Người dùng:** tất cả role.

**Mục đích:**

- Đăng nhập.
- Lấy access token, refresh token.
- Lấy user roles và permissions.
- Redirect vào dashboard.

**Frontend code:**

```text
ELog-FE/src/pages/login/LoginPage.tsx
ELog-FE/src/pages/login/LoginForm.tsx
ELog-FE/src/components/PublicRoute.tsx
ELog-FE/src/api/axiosInstance.ts
ELog-FE/src/utils/initAuth.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/AuthController.java
ELog-BE/src/main/java/com/elog/service/AuthService.java
ELog-BE/src/main/java/com/elog/service/impl/AuthServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/User.java
ELog-BE/src/main/java/com/elog/entity/Role.java
ELog-BE/src/main/java/com/elog/entity/RefreshToken.java
```

**API chính:**

```text
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

**Liên quan đề xuất xe:**

- Không trực tiếp tính xe.
- Nhưng quyết định user có được quyền vào màn import, capacity, assign, dispatch hay không.

---

### 5.2 `/dashboard` — Tổng quan

**Người dùng:** user đã đăng nhập.

**Mục đích:**

- Trang tổng quan sau login.
- Là nơi điều hướng vào các module chính.

**Frontend code:**

```text
ELog-FE/src/pages/DashboardPage.tsx
ELog-FE/src/components/AdminShell.tsx
ELog-FE/src/components/StatCard.tsx
```

**Backend code liên quan:**

```text
ELog-BE/src/main/java/com/elog/controller/DashboardController.java
```

**API liên quan:**

```text
GET /api/v1/dashboard/active-trips
GET /api/v1/trips/{id}/progress
```

**Liên quan đề xuất xe:**

- Là màn tổng quan, không phải màn lõi.
- Sau khi dispatch, dữ liệu chuyến có thể hiện ở dashboard.

---

### 5.3 `/roles` — Phân quyền

**Người dùng:** System Admin.

**Mục đích:**

- Quản lý role.
- Quản lý permission.
- Gán permission cho role.

**Frontend code:**

```text
ELog-FE/src/pages/RoleManagementPage.tsx
ELog-FE/src/api/roleApi.ts
ELog-FE/src/constants/permissions.ts
ELog-FE/src/utils/permissionChecker.ts
ELog-FE/src/components/auth/ProtectedPermissionRoute.tsx
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/RoleController.java
ELog-BE/src/main/java/com/elog/service/RoleService.java
ELog-BE/src/main/java/com/elog/service/impl/RoleServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/Role.java
ELog-BE/src/main/java/com/elog/entity/Permission.java
```

**API chính:**

```text
GET /api/v1/roles
GET /api/v1/roles/{id}
PUT /api/v1/roles/{id}/permissions
GET /api/v1/permissions
```

**Liên quan đề xuất xe:**

- Dispatcher cần permission để import, xem trip, gán xe, dispatch.
- Driver cần permission để thực hiện chuyến.
- Manager cần permission để xem monitoring/KPI.

---

### 5.4 `/users` — Quản lý người dùng

**Người dùng:** System Admin.

**Mục đích:**

- Tạo/sửa user.
- Gán role.
- Bật/tắt tài khoản.

**Frontend code:**

```text
ELog-FE/src/pages/UsersPage.tsx
ELog-FE/src/components/UserFormModal.tsx
ELog-FE/src/api/userApi.ts
ELog-FE/src/utils/userMapper.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/UserController.java
ELog-BE/src/main/java/com/elog/service/UserService.java
ELog-BE/src/main/java/com/elog/service/impl/UserServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/User.java
ELog-BE/src/main/java/com/elog/entity/Role.java
```

**API chính:**

```text
GET /api/v1/users
POST /api/v1/users
GET /api/v1/users/{id}
PUT /api/v1/users/{id}
PATCH /api/v1/users/{id}/roles
PATCH /api/v1/users/{id}/status
```

**Liên quan đề xuất xe:**

- Tạo tài khoản Dispatcher/Driver/Manager.
- Nếu user/role sai, luồng đề xuất xe không vận hành được vì không có người thao tác đúng quyền.

---

### 5.5 `/admin/drivers` — Quản lý tài xế

**Người dùng:** Admin/Dispatcher tùy permission.

**Mục đích:**

- Xem danh sách tài xế.
- Xem/cập nhật trạng thái tài xế.
- Xem lịch sử trạng thái tài xế.

**Frontend code:**

```text
ELog-FE/src/pages/admin/drivers/DriverManagementPage.tsx
ELog-FE/src/pages/admin/drivers/components/DriverStatusModal.tsx
ELog-FE/src/pages/admin/drivers/components/DriverStatusHistoryDrawer.tsx
ELog-FE/src/api/driverApi.ts
ELog-FE/src/types/driver.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/DriverStatusController.java
ELog-BE/src/main/java/com/elog/service/DriverStatusService.java
ELog-BE/src/main/java/com/elog/service/impl/DriverStatusServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/DriverStatus.java
ELog-BE/src/main/java/com/elog/entity/DriverStatusHistory.java
```

**API chính:**

```text
GET /api/v1/drivers
GET /api/v1/drivers/{id}
PATCH /api/v1/drivers/{id}/status
GET /api/v1/drivers/{id}/status-history
GET /api/v1/drivers/available
```

**Liên quan đề xuất xe:**

- Xe phù hợp nhưng không có tài xế available thì chưa thể tạo chuyến.
- Màn này quản lý điều kiện đầu vào cho assignment.

---

### 5.6 `/stores` — Quản lý cửa hàng

**Người dùng:** Admin/Dispatcher tùy permission.

**Mục đích:**

- Quản lý cửa hàng nhận hàng.
- Kiểm tra cửa hàng có route chưa.
- Kiểm tra GPS/tọa độ.

**Frontend code:**

```text
ELog-FE/src/pages/StoresPage.tsx
ELog-FE/src/api/storeApi.ts
ELog-FE/src/api/addressApi.ts
ELog-FE/src/utils/importMapper.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/StoreController.java
ELog-BE/src/main/java/com/elog/controller/AddressController.java
ELog-BE/src/main/java/com/elog/service/StoreService.java
ELog-BE/src/main/java/com/elog/service/AddressService.java
ELog-BE/src/main/java/com/elog/service/impl/StoreServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/Store.java
ELog-BE/src/main/java/com/elog/entity/Province.java
ELog-BE/src/main/java/com/elog/entity/District.java
ELog-BE/src/main/java/com/elog/entity/Ward.java
```

**API chính:**

```text
GET /api/v1/stores
POST /api/v1/stores
GET /api/v1/stores/{id}
PUT /api/v1/stores/{id}
PATCH /api/v1/stores/{id}/status
GET /api/v1/addresses/provinces
GET /api/v1/addresses/provinces/{provinceCode}/districts
GET /api/v1/addresses/districts/{districtCode}/wards
```

**Liên quan đề xuất xe:**

- Order phải giao tới store.
- Store phải thuộc route cố định.
- Store thiếu route thì order không gom chuyến được.
- Store thiếu tọa độ thì ETA/map có thể không chính xác.

---

### 5.7 `/vehicles` — Quản lý xe

**Người dùng:** Admin/Dispatcher tùy permission.

**Mục đích:**

- Quản lý đội xe.
- Xem capacity từng xe.
- Xem tổng capacity fleet.
- Xem xe available.

**Frontend code:**

```text
ELog-FE/src/pages/VehiclesPage.tsx
ELog-FE/src/api/vehicleApi.ts
ELog-FE/src/utils/numberFormat.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/VehicleController.java
ELog-BE/src/main/java/com/elog/service/VehicleService.java
ELog-BE/src/main/java/com/elog/service/impl/VehicleServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/Vehicle.java
ELog-BE/src/main/java/com/elog/entity/VehicleStatus.java
```

**API chính:**

```text
GET /api/v1/vehicles
POST /api/v1/vehicles
GET /api/v1/vehicles/{id}
PUT /api/v1/vehicles/{id}
PATCH /api/v1/vehicles/{id}/status
GET /api/v1/vehicles/fleet-capacity
GET /api/v1/vehicles/available
GET /api/v1/fleet/capacity-check
```

**Liên quan đề xuất xe:**

Đây là một trong các màn quan trọng nhất.

Recommendation cần dữ liệu:

```text
vehicle.maxWeightKg
vehicle.maxVolumeM3
vehicle.status
vehicle.availability
```

Điều kiện cơ bản:

```text
draft.totalWeightKg <= vehicle.maxWeightKg
draft.totalVolumeM3 <= vehicle.maxVolumeM3
vehicle is active/available
```

---

### 5.8 `/admin/products` — Quản lý sản phẩm

**Người dùng:** Admin.

**Mục đích:**

- Quản lý SKU/sản phẩm.
- Lưu kích thước, cân nặng, thể tích.

**Frontend code:**

```text
ELog-FE/src/pages/admin/products/ProductListPage.tsx
ELog-FE/src/pages/admin/products/ProductFormPage.tsx
ELog-FE/src/pages/admin/products/ProductDetailPage.tsx
ELog-FE/src/pages/admin/products/components/ActivateProductModal.tsx
ELog-FE/src/pages/admin/products/components/DeactivateProductModal.tsx
ELog-FE/src/api/productApi.ts
ELog-FE/src/utils/productMapper.ts
ELog-FE/src/utils/productCalculations.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/ProductController.java
ELog-BE/src/main/java/com/elog/service/ProductService.java
ELog-BE/src/main/java/com/elog/service/impl/ProductServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/Product.java
```

**API chính:**

```text
GET /api/v1/products
POST /api/v1/products
GET /api/v1/products/{id}
GET /api/v1/products/by-sku/{sku}
PUT /api/v1/products/{id}
PATCH /api/v1/products/{id}/status
```

**Liên quan đề xuất xe:**

- Product quyết định order item nặng bao nhiêu và chiếm bao nhiêu m³.
- Nếu product sai weight/volume, recommendation sẽ sai.

---

### 5.9 `/admin/routes` — Quản lý tuyến cố định

**Người dùng:** Admin/Dispatcher read-only tùy quyền.

**Mục đích:**

- Quản lý route cố định.
- Quản lý các stop trong route.
- Reorder stop.
- Activate/deactivate route.
- Xem directions/map.

**Frontend code:**

```text
ELog-FE/src/pages/admin/routes/RouteListPage.tsx
ELog-FE/src/pages/admin/routes/RouteCreatePage.tsx
ELog-FE/src/pages/admin/routes/RouteDetailPage.tsx
ELog-FE/src/pages/admin/routes/RouteEditPage.tsx
ELog-FE/src/pages/admin/routes/components/AddStoreDrawer.tsx
ELog-FE/src/pages/admin/routes/components/SortableStopItem.tsx
ELog-FE/src/pages/admin/routes/components/ActivateRouteModal.tsx
ELog-FE/src/pages/admin/routes/components/DeactivateRouteModal.tsx
ELog-FE/src/pages/admin/routes/components/DeleteStopModal.tsx
ELog-FE/src/api/routeApi.ts
ELog-FE/src/utils/routeMapper.ts
ELog-FE/src/utils/routeCalculations.ts
ELog-FE/src/utils/polyline.ts
ELog-FE/src/components/RouteMapEditor.tsx
ELog-FE/src/components/TripRouteMap.tsx
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/RouteController.java
ELog-BE/src/main/java/com/elog/service/RouteService.java
ELog-BE/src/main/java/com/elog/service/GoongMapService.java
ELog-BE/src/main/java/com/elog/service/impl/RouteServiceImpl.java
ELog-BE/src/main/java/com/elog/service/impl/GoongEtaCalculator.java
ELog-BE/src/main/java/com/elog/service/impl/HaversineEtaCalculator.java
ELog-BE/src/main/java/com/elog/entity/Route.java
ELog-BE/src/main/java/com/elog/entity/RouteStop.java
ELog-BE/src/main/java/com/elog/entity/Store.java
```

**API chính:**

```text
GET /api/v1/routes
POST /api/v1/routes
GET /api/v1/routes/{id}
PUT /api/v1/routes/{id}
PATCH /api/v1/routes/{id}/status
POST /api/v1/routes/{id}/stops
PUT /api/v1/routes/{id}/stops/reorder
DELETE /api/v1/routes/{id}/stops/{stopId}
GET /api/v1/routes/{id}/directions
```

**Liên quan đề xuất xe:**

- Route quyết định các order được gom chung.
- Stop sequence ảnh hưởng ETA, manifest và thứ tự giao.
- Hệ thống không tối ưu route động; nó dựa trên route cố định.

---

### 5.10 `/dispatcher/import` — Nhập đơn hàng Excel

**Người dùng:** Dispatcher.

**Mục đích:**

- Upload Excel chứa đơn hàng.
- Tạo ImportBatch.
- Tạo Order/OrderItem.
- Ghi nhận lỗi import.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/import/OrderImportPage.tsx
ELog-FE/src/pages/dispatcher/import/components/ImportHistoryTable.tsx
ELog-FE/src/pages/dispatcher/import/components/ImportReadOnlyBanner.tsx
ELog-FE/src/api/importApi.ts
ELog-FE/src/types/import.ts
ELog-FE/src/utils/validateExcel.ts
ELog-FE/src/utils/excelTemplate.ts
ELog-FE/src/utils/errorReport.ts
ELog-FE/src/utils/importPermissions.ts
ELog-FE/src/utils/importMapper.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/ImportController.java
ELog-BE/src/main/java/com/elog/service/ImportService.java
ELog-BE/src/main/java/com/elog/service/impl/ImportServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/ImportBatch.java
ELog-BE/src/main/java/com/elog/entity/ImportError.java
ELog-BE/src/main/java/com/elog/entity/Order.java
ELog-BE/src/main/java/com/elog/entity/OrderItem.java
```

**API chính:**

```text
POST /api/v1/imports
GET /api/v1/imports
GET /api/v1/imports/{batchId}
GET /api/v1/imports/{batchId}/orders
GET /api/v1/imports/{batchId}/errors
GET /api/v1/imports/{batchId}/errors/export
```

**Liên quan đề xuất xe:**

Đây là đầu vào của toàn bộ thuật toán đề xuất xe.

Nếu Excel sai, các bước sau đều sai:

```text
Excel sai SKU → product weight/volume không map được
Excel sai store → không biết giao tới đâu
Store chưa có route → không gom được trip draft
```

---

### 5.11 `/dispatcher/import/history/:batchId` — Chi tiết batch import

**Người dùng:** Dispatcher/Manager read-only tùy quyền.

**Mục đích:**

- Xem một lần import cụ thể.
- Xem order đã tạo.
- Xem lỗi từng dòng.
- Export lỗi.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/import/ImportBatchDetailPage.tsx
ELog-FE/src/api/importApi.ts
ELog-FE/src/types/import.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/ImportController.java
ELog-BE/src/main/java/com/elog/service/impl/ImportServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/ImportBatch.java
ELog-BE/src/main/java/com/elog/entity/ImportError.java
```

**Liên quan đề xuất xe:**

- Giúp làm sạch dữ liệu trước khi tạo trip draft.
- Nếu batch là partial, Dispatcher cần xem lỗi trước khi tiếp tục.

---

### 5.12 `/dispatcher/trip-drafts` — Danh sách Trip Draft

**Người dùng:** Dispatcher/Manager.

**Mục đích:**

- Xem các bản nháp chuyến.
- Tạo/gom đơn thành draft.
- Vào chi tiết draft.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/trip-drafts/TripDraftListPage.tsx
ELog-FE/src/api/tripDraftApi.ts
ELog-FE/src/types/tripDraft.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/TripDraftController.java
ELog-BE/src/main/java/com/elog/service/TripDraftService.java
ELog-BE/src/main/java/com/elog/service/impl/TripDraftServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/TripDraft.java
ELog-BE/src/main/java/com/elog/entity/TripDraftStop.java
ELog-BE/src/main/java/com/elog/entity/Order.java
```

**API chính:**

```text
POST /api/v1/trip-drafts/consolidate
GET /api/v1/trip-drafts
GET /api/v1/trip-drafts/{id}
```

**Liên quan đề xuất xe:**

- Trip Draft là đối tượng mà hệ thống sẽ tính capacity và đề xuất xe.
- Không có draft thì không có recommendation.

---

### 5.13 `/dispatcher/trip-drafts/:id` — Chi tiết Trip Draft

**Người dùng:** Dispatcher/Manager.

**Mục đích:**

- Xem route, stop, order, item của draft.
- Xem tổng tải.
- Xem ETA.
- Confirm/revert.
- Exclude/re-include order nếu cần.
- Xem history.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/trip-drafts/TripDraftDetailPage.tsx
ELog-FE/src/api/tripDraftApi.ts
ELog-FE/src/api/planningHistoryApi.ts
ELog-FE/src/types/tripDraft.ts
ELog-FE/src/types/planningEvent.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/TripDraftController.java
ELog-BE/src/main/java/com/elog/service/TripDraftService.java
ELog-BE/src/main/java/com/elog/service/DepartureAdjustmentService.java
ELog-BE/src/main/java/com/elog/service/PlanningHistoryService.java
ELog-BE/src/main/java/com/elog/service/impl/TripDraftServiceImpl.java
ELog-BE/src/main/java/com/elog/service/impl/DepartureAdjustmentServiceImpl.java
ELog-BE/src/main/java/com/elog/service/impl/PlanningHistoryServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/TripDraft.java
ELog-BE/src/main/java/com/elog/entity/TripDraftStop.java
ELog-BE/src/main/java/com/elog/entity/TripPlanningEvent.java
```

**API chính:**

```text
GET /api/v1/trip-drafts/{id}
GET /api/v1/trip-drafts/{id}/stops
PATCH /api/v1/trip-drafts/{id}/stops/{stopId}
GET /api/v1/trip-drafts/{id}/stops/{stopId}/order-items
POST /api/v1/trip-drafts/{id}/recalculate-eta
POST /api/v1/trip-drafts/{id}/confirm
POST /api/v1/trip-drafts/{id}/revert
POST /api/v1/trip-drafts/{id}/optimal-departure
POST /api/v1/trip-drafts/{id}/adjust-departure-time
POST /api/v1/trip-drafts/{id}/orders/{orderId}/settle-delay
POST /api/v1/trip-drafts/{id}/orders/{orderId}/exclude
POST /api/v1/trip-drafts/{id}/orders/{orderId}/re-include
GET /api/v1/trip-drafts/{id}/excluded-orders
GET /api/v1/trip-drafts/{id}/history
```

**Liên quan đề xuất xe:**

- Đây là nơi kiểm tra bản nháp trước khi hỏi "xe nào phù hợp?".
- Draft càng chính xác thì recommendation càng đáng tin.

---

### 5.14 `/dispatcher/trip-drafts/:id/capacity` — Capacity Validation

**Người dùng:** Dispatcher.

**Mục đích:**

- Kiểm tra draft có vừa xe/fleet không.
- Xem kết quả validation.
- Sinh manifest nếu hợp lệ.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/trip-drafts/CapacityValidationPage.tsx
ELog-FE/src/api/tripDraftApi.ts
ELog-FE/src/api/loadingManifestApi.ts
ELog-FE/src/types/tripDraft.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/TripDraftController.java
ELog-BE/src/main/java/com/elog/service/CapacityValidationService.java
ELog-BE/src/main/java/com/elog/service/ConstraintValidationService.java
ELog-BE/src/main/java/com/elog/service/ManifestService.java
ELog-BE/src/main/java/com/elog/service/impl/CapacityValidationServiceImpl.java
ELog-BE/src/main/java/com/elog/service/impl/ConstraintValidationServiceImpl.java
ELog-BE/src/main/java/com/elog/service/impl/ManifestServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/ConstraintResult.java
ELog-BE/src/main/java/com/elog/entity/Manifest.java
ELog-BE/src/main/java/com/elog/entity/ManifestLine.java
```

**API chính:**

```text
POST /api/v1/trip-drafts/{id}/validate-capacity
GET /api/v1/trip-drafts/{id}/validation-result
POST /api/v1/trip-drafts/{id}/generate-manifest
GET /api/v1/trip-drafts/{id}/manifest
GET /api/v1/trip-drafts/{id}/manifest/by-stop
```

**Liên quan đề xuất xe:**

Đây là lõi kỹ thuật của đề xuất xe.

Logic bắt buộc:

```text
draft.totalWeightKg <= vehicle.maxWeightKg
draft.totalVolumeM3 <= vehicle.maxVolumeM3
```

Không được chỉ check một chiều. Nếu một chiều fail thì xe không phù hợp.

---

### 5.15 `/dispatcher/trip-drafts/:id/assign` — Gán xe/tài xế

**Người dùng:** Dispatcher.

**Mục đích:**

- Xem xe đủ điều kiện.
- Xem tài xế available.
- Chọn phương án xe.
- Gán một xe hoặc split nhiều xe.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/trip-drafts/VehicleAssignmentPage.tsx
ELog-FE/src/api/tripApi.ts
ELog-FE/src/api/tripDraftApi.ts
ELog-FE/src/api/vehicleApi.ts
ELog-FE/src/api/driverApi.ts
ELog-FE/src/api/recommendationNormalizer.ts
ELog-FE/src/types/trip.ts
ELog-FE/src/types/tripDraft.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/TripController.java
ELog-BE/src/main/java/com/elog/service/TripService.java
ELog-BE/src/main/java/com/elog/service/RecommendationService.java
ELog-BE/src/main/java/com/elog/service/VehicleService.java
ELog-BE/src/main/java/com/elog/service/DriverStatusService.java
ELog-BE/src/main/java/com/elog/service/impl/TripServiceImpl.java
ELog-BE/src/main/java/com/elog/service/impl/RecommendationServiceImpl.java
ELog-BE/src/main/java/com/elog/service/impl/VehicleServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/Vehicle.java
ELog-BE/src/main/java/com/elog/entity/Trip.java
ELog-BE/src/main/java/com/elog/entity/TripDraft.java
```

**API chính:**

```text
GET /api/v1/trip-drafts/{id}/eligible-vehicles
GET /api/v1/trip-drafts/{id}/eligible-vehicles-for-stops
GET /api/v1/drivers/available
GET /api/v1/trip-drafts/{id}/recommendations
POST /api/v1/trip-drafts/{id}/assign
POST /api/v1/trip-drafts/{id}/assign-split
PATCH /api/v1/trips/{id}/assignment
```

**Liên quan đề xuất xe:**

Đây là màn quan trọng nhất khi demo "vehicle recommendation".

Màn này trả lời:

```text
Xe nào đủ kg?
Xe nào đủ m³?
Xe nào đang available?
Xe nào tối ưu hơn?
Có cần split không?
Tài xế nào có thể chạy?
```

---

### 5.16 Loading Manifest — Phiếu bốc hàng LIFO

Routes:

```text
/trip-drafts/:tripDraftId/loading-manifest
/trips/:tripId/loading-manifest
```

**Người dùng:** Dispatcher/Warehouse/Manager.

**Mục đích:**

- Xem phiếu bốc hàng.
- Xem thứ tự hàng theo LIFO.
- Xem theo stop hoặc flat list.

**Frontend code:**

```text
ELog-FE/src/pages/LifoManifestPage.tsx
ELog-FE/src/components/manifest/ManifestSummary.tsx
ELog-FE/src/components/manifest/FlatManifestView.tsx
ELog-FE/src/components/manifest/ByStopManifestView.tsx
ELog-FE/src/api/loadingManifestApi.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/TripDraftController.java
ELog-BE/src/main/java/com/elog/service/ManifestService.java
ELog-BE/src/main/java/com/elog/service/impl/ManifestServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/Manifest.java
ELog-BE/src/main/java/com/elog/entity/ManifestLine.java
```

**API chính:**

```text
POST /api/v1/trip-drafts/{id}/generate-manifest
GET /api/v1/trip-drafts/{id}/manifest
GET /api/v1/trip-drafts/{id}/manifest/by-stop
GET /api/v1/trips/{id}/handover-slip
```

**Liên quan đề xuất xe:**

- Sau khi chọn xe, hệ thống phải biết hàng nào xếp lên xe đó.
- LIFO đảm bảo điểm giao đầu lấy hàng ra trước.

---

### 5.17 `/dispatcher/trips/:tripId/dispatch` — Dispatch chuyến

**Người dùng:** Dispatcher.

**Mục đích:**

- Chốt chuyến.
- Kiểm tra xe/tài xế/manifest đã sẵn sàng.
- Chuyển trip sang trạng thái dispatched.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/trips/DispatchPage.tsx
ELog-FE/src/api/tripApi.ts
ELog-FE/src/types/trip.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/TripController.java
ELog-BE/src/main/java/com/elog/service/TripService.java
ELog-BE/src/main/java/com/elog/service/TripStateMachine.java
ELog-BE/src/main/java/com/elog/service/impl/TripServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/Trip.java
ELog-BE/src/main/java/com/elog/entity/TripStatus.java
```

**API chính:**

```text
GET /api/v1/trips
GET /api/v1/trips/{tripId}
POST /api/v1/trips/{id}/dispatch
GET /api/v1/trips/{id}/handover-slip
```

**Liên quan đề xuất xe:**

- Dispatch chỉ nên xảy ra sau khi xe/tài xế đã được gán hợp lệ.
- Đây là bước biến recommendation/assignment thành chuyến vận hành thật.

---

### 5.18 `/dispatcher/monitoring` và `/manager/monitoring` — Theo dõi chuyến

**Người dùng:** Dispatcher, Logistics Manager.

**Mục đích:**

- Theo dõi chuyến đang chạy.
- Xem tiến độ, ETA, trạng thái stop.
- Phát hiện chuyến trễ.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/monitoring/MonitoringDashboardPage.tsx
ELog-FE/src/guards/MonitoringGuard.tsx
ELog-FE/src/api/monitoringApi.ts
ELog-FE/src/types/monitoring.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/TripMonitoringController.java
ELog-BE/src/main/java/com/elog/controller/DashboardController.java
ELog-BE/src/main/java/com/elog/service/TripMonitoringService.java
ELog-BE/src/main/java/com/elog/service/impl/TripMonitoringServiceImpl.java
ELog-BE/src/main/java/com/elog/service/impl/TimeExceptionDetectionJob.java
ELog-BE/src/main/java/com/elog/entity/Trip.java
ELog-BE/src/main/java/com/elog/entity/TripStop.java
ELog-BE/src/main/java/com/elog/entity/TripStopStatus.java
```

**API chính:**

```text
GET /api/v1/dashboard/active-trips
GET /api/v1/trips/{id}/progress
POST /api/v1/trips/{id}/start
POST /api/v1/trip-stops/{id}/arrive
POST /api/v1/trip-stops/{id}/complete
```

**Liên quan đề xuất xe:**

- Monitoring là nơi kiểm tra quyết định gán xe có vận hành ổn không.
- Nếu xe được chọn không phù hợp, có thể sinh trễ/exception.

---

### 5.19 `/dispatcher/exceptions` và `/manager/exceptions` — Quản lý ngoại lệ

**Người dùng:** Dispatcher, Logistics Manager.

**Mục đích:**

- Xem các vấn đề phát sinh trong chuyến.
- Xử lý/resolve exception.
- Xem violation.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/exceptions/ExceptionManagementPage.tsx
ELog-FE/src/guards/ExceptionGuard.tsx
ELog-FE/src/api/exceptionApi.ts
ELog-FE/src/types/exception.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/ExceptionController.java
ELog-BE/src/main/java/com/elog/service/ExceptionService.java
ELog-BE/src/main/java/com/elog/service/impl/ExceptionServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/DeliveryException.java
ELog-BE/src/main/java/com/elog/entity/ExceptionType.java
```

**API chính:**

```text
GET /api/v1/exceptions
GET /api/v1/exceptions/{id}
PATCH /api/v1/exceptions/{id}/resolve
GET /api/v1/exceptions/violations
POST /api/v1/trip-stops/{id}/reject
```

**Liên quan đề xuất xe:**

- Nếu recommendation/gán xe không phù hợp, ngoại lệ có thể tăng.
- Dữ liệu exception là feedback để đánh giá chất lượng điều phối.

---

### 5.20 `/dispatcher/trip-outcomes` và `/manager/trip-outcomes` — Kết quả chuyến

**Người dùng:** Dispatcher, Logistics Manager.

**Mục đích:**

- Xem kết quả chuyến sau giao.
- Validate/amend outcome.
- Xem lịch sử outcome.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/outcomes/TripOutcomePage.tsx
ELog-FE/src/api/tripOutcomeApi.ts
ELog-FE/src/api/tripOutcomeEventApi.ts
ELog-FE/src/types/tripOutcome.ts
ELog-FE/src/types/tripOutcomeEvent.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/TripOutcomeController.java
ELog-BE/src/main/java/com/elog/controller/TripOutcomeHistoryController.java
ELog-BE/src/main/java/com/elog/service/TripOutcomeService.java
ELog-BE/src/main/java/com/elog/service/TripOutcomeHistoryService.java
ELog-BE/src/main/java/com/elog/service/impl/TripOutcomeServiceImpl.java
ELog-BE/src/main/java/com/elog/service/impl/TripOutcomeHistoryServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/TripOutcome.java
ELog-BE/src/main/java/com/elog/entity/TripOutcomeEvent.java
```

**API chính:**

```text
GET /api/v1/trip-outcomes
POST /api/v1/trip-outcomes/{id}/validate
POST /api/v1/trip-outcomes/{id}/amend
GET /api/v1/trip-outcome-events
GET /api/v1/trips/{tripId}/outcome-history
```

**Liên quan đề xuất xe:**

- Outcome cho biết chuyến sau khi gán xe có thành công không.
- Đây là dữ liệu sau vận hành để đánh giá recommendation.

---

### 5.21 `/dispatcher/kpi` và `/manager/kpi` — KPI vận hành

**Người dùng:** Dispatcher, Logistics Manager.

**Mục đích:**

- Xem hiệu suất vận hành.
- Xem KPI theo route, vehicle, driver.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/kpi/KpiDashboardPage.tsx
ELog-FE/src/api/kpiApi.ts
ELog-FE/src/types/kpi.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/KpiController.java
ELog-BE/src/main/java/com/elog/service/KpiService.java
ELog-BE/src/main/java/com/elog/service/impl/KpiServiceImpl.java
```

**API chính:**

```text
GET /api/v1/kpi/summary
GET /api/v1/kpi/daily-trend
GET /api/v1/kpi/by-route
GET /api/v1/kpi/by-vehicle
GET /api/v1/kpi/by-driver
```

**Liên quan đề xuất xe:**

KPI là nơi chứng minh đề xuất xe có giá trị:

```text
Tỉ lệ giao thành công
Tỉ lệ trễ
Hiệu suất xe
Hiệu suất tài xế
Route nào hay phát sinh vấn đề
```

---

### 5.22 `/dispatcher/activity-history` và `/manager/activity-history` — Nhật ký hoạt động

**Người dùng:** Dispatcher, Logistics Manager.

**Mục đích:**

- Xem lịch sử lập kế hoạch.
- Audit ai làm gì, lúc nào.

**Frontend code:**

```text
ELog-FE/src/pages/dispatcher/activity-history/ActivityHistoryPage.tsx
ELog-FE/src/api/planningHistoryApi.ts
ELog-FE/src/types/planningEvent.ts
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/PlanningHistoryController.java
ELog-BE/src/main/java/com/elog/service/PlanningHistoryService.java
ELog-BE/src/main/java/com/elog/service/impl/PlanningHistoryServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/TripPlanningEvent.java
ELog-BE/src/main/java/com/elog/entity/PlanningEventType.java
ELog-BE/src/main/java/com/elog/entity/PlanningActorType.java
```

**API chính:**

```text
GET /api/v1/planning-events
GET /api/v1/trip-drafts/{id}/history
```

**Liên quan đề xuất xe:**

- Cho biết ai confirm draft, ai gán xe, ai dispatch.
- Nếu Dispatcher override recommendation, history cần ghi lại để audit.

---

### 5.23 `/driver/my-trips` — Chuyến của tôi

**Người dùng:** Driver.

**Mục đích:**

- Driver xem chuyến được giao.
- Start trip.
- Đánh dấu đến stop.
- Ghi kết quả từng order.
- Complete trip.
- Return to warehouse.

**Frontend web code:**

```text
ELog-FE/src/pages/driver/DriverMyTripsPage.tsx
ELog-FE/src/pages/driver/OrderResultModal.tsx
ELog-FE/src/pages/driver/DeliveryRejectionModal.tsx
ELog-FE/src/guards/DriverGuard.tsx
ELog-FE/src/api/driverApi.ts
ELog-FE/src/types/driverTrip.ts
```

**Mobile code:**

```text
Elog-Mobile/integration_test/report5_l4_mobile_test.dart
Elog-Mobile/TESTING_REPORT5.md
```

**Backend code:**

```text
ELog-BE/src/main/java/com/elog/controller/DriverTripController.java
ELog-BE/src/main/java/com/elog/service/DriverTripService.java
ELog-BE/src/main/java/com/elog/service/impl/DriverTripServiceImpl.java
ELog-BE/src/main/java/com/elog/entity/TripExecution.java
ELog-BE/src/main/java/com/elog/entity/DeliveryOrderResult.java
ELog-BE/src/main/java/com/elog/entity/DeliveryException.java
```

**API chính:**

```text
GET /api/v1/driver/trips/active
GET /api/v1/driver/trips/pending-return
POST /api/v1/driver/trips/{executionId}/start
POST /api/v1/driver/trips/{executionId}/stops/{stopId}/arrive
PUT /api/v1/driver/trips/{executionId}/orders/{orderId}/result
POST /api/v1/driver/trips/{executionId}/complete
POST /api/v1/driver/trips/{executionId}/return-to-warehouse
```

**Liên quan đề xuất xe:**

- Driver là người thực thi kết quả sau assignment.
- Nếu gán xe/tài xế đúng, driver flow chạy mượt.
- Nếu gán sai, exception/outcome sẽ phản ánh.

---

### 5.24 `/403` — Không có quyền

**Người dùng:** user thiếu permission.

**Mục đích:**

- Chặn truy cập màn không có quyền.

**Frontend code:**

```text
ELog-FE/src/pages/ForbiddenPage.tsx
ELog-FE/src/components/auth/ProtectedPermissionRoute.tsx
ELog-FE/src/components/ProtectedRoute.tsx
ELog-FE/src/guards/*.tsx
```

**Backend liên quan:**

```text
ELog-BE/src/main/java/com/elog/config
```

**Liên quan đề xuất xe:**

- Ngăn user không có quyền vào màn assign/dispatch.

---

### 5.25 Not Found

**Mục đích:**

- Hiển thị khi URL không tồn tại.

**Frontend code:**

```text
ELog-FE/src/pages/NotFoundPage.tsx
ELog-FE/src/App.tsx
```

---

## 6. Module không phải màn hình nhưng rất quan trọng

### 6.1 `AdminShell`

Code:

```text
ELog-FE/src/components/AdminShell.tsx
```

Vai trò:

- Layout chính của app.
- Sidebar menu.
- Header.
- Logout.
- Hiển thị menu theo permission.

Điểm cần nhớ:

```text
User có permission nào thì menu tương ứng mới hiện.
```

### 6.2 Route protection

Code:

```text
ELog-FE/src/components/ProtectedRoute.tsx
ELog-FE/src/components/PublicRoute.tsx
ELog-FE/src/components/auth/ProtectedPermissionRoute.tsx
ELog-FE/src/guards/MonitoringGuard.tsx
ELog-FE/src/guards/ExceptionGuard.tsx
ELog-FE/src/guards/DriverGuard.tsx
ELog-FE/src/guards/ImportModuleGuard.tsx
ELog-FE/src/guards/RouteManagementGuard.tsx
```

Vai trò:

- Chặn user chưa login.
- Chặn user không có permission.
- Redirect về `/login` hoặc `/403`.

### 6.3 Axios layer

Code:

```text
ELog-FE/src/api/axiosInstance.ts
```

Vai trò:

- Gắn base URL.
- Gắn Authorization token.
- Xử lý response/error chung.

### 6.4 Permission constants

Code:

```text
ELog-FE/src/constants/permissions.ts
ELog-FE/src/utils/permissionChecker.ts
ELog-FE/src/hooks/usePermissions.ts
```

Vai trò:

- Định nghĩa permission string.
- Kiểm tra user có quyền nào.
- Dùng trong menu và route guard.

---

## 7. Các service backend quan trọng nhất cho lõi đề xuất xe

### 7.1 `ImportServiceImpl`

Code:

```text
ELog-BE/src/main/java/com/elog/service/impl/ImportServiceImpl.java
```

Vai trò:

- Đọc file Excel.
- Validate dòng dữ liệu.
- Tạo ImportBatch.
- Tạo Order/OrderItem.
- Ghi ImportError.

Liên quan recommendation:

- Tạo order sạch để gom draft.

### 7.2 `TripDraftServiceImpl`

Code:

```text
ELog-BE/src/main/java/com/elog/service/impl/TripDraftServiceImpl.java
```

Vai trò:

- Gom order thành trip draft.
- Quản lý stop/order trong draft.
- Confirm/revert draft.
- Recalculate ETA.
- Exclude/re-include order.

Liên quan recommendation:

- Tạo đối tượng đầu vào trực tiếp cho capacity/recommendation.

### 7.3 `CapacityValidationServiceImpl`

Code:

```text
ELog-BE/src/main/java/com/elog/service/impl/CapacityValidationServiceImpl.java
```

Vai trò:

- Tính tổng weight/volume.
- Kiểm tra draft có vừa capacity không.
- Kiểm tra fleet capacity.

Liên quan recommendation:

- Đây là lõi rule kg/m³.

### 7.4 `RecommendationServiceImpl`

Code:

```text
ELog-BE/src/main/java/com/elog/service/impl/RecommendationServiceImpl.java
```

Vai trò:

- Tạo danh sách phương án xe.
- Đánh giá xe phù hợp.
- Hỗ trợ split/vehicle recommendation.

Liên quan recommendation:

- Đây là engine đề xuất xe.

### 7.5 `TripServiceImpl`

Code:

```text
ELog-BE/src/main/java/com/elog/service/impl/TripServiceImpl.java
```

Vai trò:

- Gán xe/tài xế.
- Assign split.
- Dispatch trip.
- Quản lý trip state.

Liên quan recommendation:

- Biến đề xuất thành assignment thật.

### 7.6 `ManifestServiceImpl`

Code:

```text
ELog-BE/src/main/java/com/elog/service/impl/ManifestServiceImpl.java
```

Vai trò:

- Sinh loading manifest.
- Tính thứ tự LIFO.

Liên quan recommendation:

- Sau khi có phương án xe/chuyến, manifest cho kho biết cách xếp hàng.

### 7.7 `DriverTripServiceImpl`

Code:

```text
ELog-BE/src/main/java/com/elog/service/impl/DriverTripServiceImpl.java
```

Vai trò:

- Driver start trip.
- Driver arrive stop.
- Driver ghi order result.
- Driver complete/return.

Liên quan recommendation:

- Thực thi chuyến sau khi xe đã được đề xuất/gán.

### 7.8 `KpiServiceImpl`

Code:

```text
ELog-BE/src/main/java/com/elog/service/impl/KpiServiceImpl.java
```

Vai trò:

- Tính KPI theo route/vehicle/driver.

Liên quan recommendation:

- Đo hiệu quả sau khi sử dụng xe được đề xuất.

---

## 8. Entity map theo nghiệp vụ

| Entity | Vai trò |
|---|---|
| `User` | Tài khoản người dùng |
| `Role` | Nhóm quyền |
| `Permission` | Quyền cụ thể |
| `DriverStatus` | Trạng thái tài xế |
| `DriverStatusHistory` | Lịch sử trạng thái tài xế |
| `Store` | Cửa hàng nhận hàng |
| `Route` | Tuyến cố định |
| `RouteStop` | Điểm dừng trong tuyến |
| `Vehicle` | Xe |
| `VehicleStatus` | Trạng thái xe |
| `Product` | Sản phẩm/SKU |
| `ImportBatch` | Một lần import Excel |
| `ImportError` | Lỗi import |
| `Order` | Đơn hàng |
| `OrderItem` | Dòng hàng trong đơn |
| `TripDraft` | Bản nháp chuyến |
| `TripDraftStop` | Stop trong draft |
| `ConstraintResult` | Kết quả kiểm tra constraint/capacity |
| `Trip` | Chuyến giao thật |
| `TripStatus` | Trạng thái chuyến |
| `TripStop` | Điểm dừng trong chuyến thật |
| `TripExecution` | Phiên thực thi chuyến bởi driver |
| `Manifest` | Phiếu bốc hàng |
| `ManifestLine` | Dòng hàng trong manifest |
| `DeliveryException` | Ngoại lệ giao hàng |
| `DeliveryOrderResult` | Kết quả giao từng order |
| `TripOutcome` | Kết quả chuyến |
| `TripOutcomeEvent` | Lịch sử outcome |
| `TripPlanningEvent` | Lịch sử lập kế hoạch |

---

## 9. Test/report code liên quan

### 9.1 Backend Report 5

```text
ELog-BE/test-execution/catalog/l1.json
ELog-BE/test-execution/catalog/l2.json
ELog-BE/test-execution/catalog/l3.json
ELog-BE/test-execution/catalog/l4.json
ELog-BE/test-execution/catalog/uat.json

ELog-BE/test-execution/results/all-results.json
ELog-BE/test-execution/results/l1.json
ELog-BE/test-execution/results/l2.json
ELog-BE/test-execution/results/l3.json

ELog-BE/test-execution/REPORT5_TEST_STATUS.md
ELog-BE/test-execution/BUG_REPORT.md
```

### 9.2 L1 Unit tests

```text
ELog-BE/src/test/java/com/elog/service/*Report5Test.java
```

Bao phủ service-level logic như:

- Auth.
- User.
- Role.
- Product.
- Vehicle.
- Route.
- Import.
- Trip draft.
- Capacity.
- Recommendation.
- Manifest.
- Trip state.

### 9.3 L2 Integration tests

```text
ELog-BE/src/test/java/com/elog/integration/*Report5IntegrationTest.java
```

Bao phủ integration service/repository/database.

### 9.4 L3 System API tests

```text
ELog-BE/test-execution/scripts/run-l3-api.mjs
ELog-BE/test-execution/scripts/l3-runner-lib.mjs
ELog-BE/test-execution/evidence/l3-rerun-results.json
```

Bao phủ API thật.

### 9.5 L4 Cypress E2E

```text
ELog-FE/src/Test/e2e/l4/report5-web.cy.ts
ELog-FE/src/Test/e2e/l4/build-l4-results.mjs
ELog-FE/src/Test/e2e/l4/support/l4Catalog.mjs
ELog-FE/src/Test/e2e/l4/support/report5Fixture.mjs
ELog-FE/src/Test/scripts/open-report5-l4-cypress-ui.ps1
ELog-FE/REPORT5_L4_CYPRESS_UI_RUNBOOK.md
```

Chạy Cypress UI:

```powershell
cd D:\Elog\ELog-FE\src\Test
npm run cy:open:l4
```

### 9.6 Mobile smoke/UAT preparation

```text
Elog-Mobile/integration_test/report5_l4_mobile_test.dart
Elog-Mobile/TESTING_REPORT5.md
```

---

## 10. Nếu cần demo hệ thống theo đúng câu chuyện đề xuất xe

Thứ tự demo nên là:

1. Login.
2. Vào Product để giải thích sản phẩm có weight/volume.
3. Vào Vehicle để giải thích xe có capacity kg/m³.
4. Vào Store/Route để giải thích đơn được gom theo tuyến cố định.
5. Vào Import để upload Excel.
6. Vào Trip Draft để xem hệ thống gom đơn.
7. Vào Capacity để kiểm tra tải.
8. Vào Assign để xem đề xuất/gán xe.
9. Vào Manifest để giải thích LIFO.
10. Vào Dispatch để chốt chuyến.
11. Vào Driver My Trips để tài xế chạy chuyến.
12. Vào Monitoring/Exception để xem vận hành.
13. Vào KPI/Activity History để xem hiệu quả và audit.

Câu nói demo ngắn:

> ELog tập trung vào bài toán đề xuất xe cho giao hàng. Hệ thống nhận đơn từ Excel, gom theo tuyến cố định, tính tổng khối lượng và thể tích, kiểm tra năng lực xe, đề xuất phương án xe/tài xế phù hợp, rồi hỗ trợ Dispatcher dispatch, Driver giao hàng và Manager theo dõi KPI/exception.

---

## 11. Bản đồ ngắn: màn hình nào quan trọng nhất cho đề xuất xe?

| Mức độ | Màn hình | Vì sao quan trọng |
|---|---|---|
| Rất cao | `/dispatcher/trip-drafts/:id/assign` | Nơi hiển thị/gán xe đề xuất |
| Rất cao | `/dispatcher/trip-drafts/:id/capacity` | Nơi kiểm tra kg/m³ |
| Rất cao | `/vehicles` | Nguồn dữ liệu capacity xe |
| Rất cao | `/admin/products` | Nguồn dữ liệu weight/volume hàng |
| Cao | `/dispatcher/import` | Nguồn đơn hàng đầu vào |
| Cao | `/dispatcher/trip-drafts/:id` | Nguồn dữ liệu draft để đề xuất |
| Cao | `/admin/routes` | Store được gom theo tuyến |
| Cao | `/stores` | Store phải hợp lệ và có route |
| Trung bình | `/dispatcher/trips/:tripId/dispatch` | Chốt chuyến sau khi gán xe |
| Trung bình | `/dispatcher/kpi` | Đánh giá hiệu quả sau vận hành |

---

## 12. Checklist khi sửa hoặc kiểm tra chức năng đề xuất xe

Khi có bug liên quan đề xuất xe, kiểm theo thứ tự:

1. Excel có import đúng order không?
2. SKU trong Excel có map đúng Product không?
3. Product có đúng weight/volume không?
4. Store trong order có tồn tại không?
5. Store có thuộc Route active không?
6. Trip Draft có gom đúng order/stop không?
7. Tổng kg/m³ trong draft có đúng không?
8. Vehicle có active/available không?
9. Vehicle capacity kg/m³ có đúng không?
10. CapacityValidationService có pass/fail đúng không?
11. RecommendationService có trả đúng danh sách xe không?
12. FE `recommendationNormalizer` có map response đúng không?
13. Màn `VehicleAssignmentPage` có hiển thị đúng không?
14. Assign API có tạo Trip/assignment đúng không?
15. Dispatch có bị block nếu thiếu xe/tài xế/manifest không?
16. Monitoring/outcome có phản ánh chuyến sau dispatch không?

Nếu một đề xuất xe nhìn "sai", đừng sửa ngay màn hình. Hãy lần theo chuỗi trên, vì lỗi có thể nằm ở data nền chứ không phải recommendation engine.

