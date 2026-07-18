# Báo cáo Kết quả Kiểm thử & Độ bao phủ (Backend)

* **Ngày thực hiện:** 18/07/2026
* **Tổng số test cases:** 278
* **Số test case đạt (Passed):** 278 (100% Pass Rate)
* **Số test case bị bỏ qua (Skipped):** 9 (các integration test môi trường staging/production cấu hình skip mặc định khi chạy local)
* **Trạng thái Build:** BUILD SUCCESS
* **Công cụ đo độ bao phủ:** JaCoCo

---

## 📊 Bảng Độ bao phủ Dòng lệnh (Line Coverage) các lớp Service lõi

Dưới đây là thống kê độ bao phủ dòng lệnh của các Service Implementation thuộc package `com.elog.service.impl`:

| Tên lớp (Service Class Name) | Số dòng chưa bao phủ | Số dòng đã bao phủ | Tổng số dòng | Tỷ lệ bao phủ dòng (Line Coverage) | Trạng thái (>90%) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **AuthServiceImpl** | 0 | 46 | 46 | **100.00%** | Pass |
| **StoreServiceImpl** | 0 | 57 | 57 | **100.00%** | Pass |
| **VehicleServiceImpl** | 0 | 45 | 45 | **100.00%** | Pass |
| **RouteServiceImpl** | 0 | 105 | 105 | **100.00%** | Pass |
| **ProductServiceImpl** | 0 | 46 | 46 | **100.00%** | Pass |
| **UserServiceImpl** | 0 | 65 | 65 | **100.00%** | Pass |
| **ImportServiceImpl** | 1 | 278 | 279 | **99.64%** | Pass |
| **ManifestServiceImpl** | 4 | 177 | 181 | **97.79%** | Pass |
| **CapacityValidationServiceImpl** | 9 | 177 | 186 | **95.16%** | Pass |
| **TripMonitoringServiceImpl** | 23 | 250 | 273 | **91.58%** | Pass |
| **TripServiceImpl** | 35 | 376 | 411 | **91.48%** | Pass |
| **ExceptionServiceImpl** | 17 | 156 | 173 | **90.17%** | Pass |

> [!NOTE]
> Tất cả các lớp dịch vụ nghiệp vụ chính (Service Implementations) liên quan đến Sprint 2 & Sprint 3 đều đã được bổ sung test case toàn diện để vượt mức yêu cầu **90%** coverage.

---

## 📂 Danh mục tài liệu lưu trữ làm bằng chứng (Evidence)

* **Báo cáo chi tiết dạng HTML (JaCoCo):** Đã được sao chép đầy đủ về thư mục [jacoco-report/](file:///D:/FULearning/semester%209/Elog/Evidence/backend-tests/jacoco-report/index.html) để kiểm tra trực quan từng dòng code được bao phủ.
* **Các file Unit Test được chỉnh sửa và viết mới:**
  * [ImportServiceImplTest.java](file:///D:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/ImportServiceImplTest.java) (Sửa lỗi khớp verify)
  * [CapacityValidationServiceImplTest.java](file:///D:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/CapacityValidationServiceImplTest.java) (Tạo mới)
  * [ManifestServiceImplTest.java](file:///D:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/ManifestServiceImplTest.java) (Tạo mới)
  * [TripServiceImplTest.java](file:///D:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/TripServiceImplTest.java) (Tạo mới & mở rộng)
  * [TripMonitoringServiceImplTest.java](file:///D:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/TripMonitoringServiceImplTest.java) (Tạo mới & mở rộng)
  * [ExceptionServiceImplTest.java](file:///D:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/ExceptionServiceImplTest.java) (Tạo mới & mở rộng)
