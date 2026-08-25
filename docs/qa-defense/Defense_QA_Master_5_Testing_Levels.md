# 🎓 TÀI LIỆU TOÀN DIỆN: 150 CÂU HỎI & TRẢ LỜI PHẢN BIỆN BẢO VỆ ĐỒ ÁN 5 TẦNG KIỂM THỬ (L1 – L5/UAT)

**Dự án**: ELog Delivery Management System  
**Tác giả**: Ban thẩm định chất lượng QA & Kỹ sư phát triển ELog  
**Vai trò mô phỏng**: 
- 👨‍🏫 **Hội đồng chấm thi / Chuyên gia QA 10 năm kinh nghiệm** (Đặt câu hỏi phản biện, soi xét kỹ thuật, kiểm tra tính trung thực và độ tin cậy của kiểm thử).
- 👨‍🎓 **Sinh viên / Kỹ sư bảo vệ đồ án** (Trả lời tự tin, mạch lạc, dẫn chứng cụ thể bằng Code, Bằng chứng Execution, Báo cáo kiểm thử, và Dòng lệnh thực tế).

---

## 📑 MỤC LỤC

1. [Phần 1: L1 — Unit Testing (30 Câu hỏi & Bằng chứng)](#phần-1-l1--unit-testing-30-câu-hỏi--bằng-chứng)
2. [Phần 2: L2 — Integration Testing (30 Câu hỏi & Bằng chứng)](#phần-2-l2--integration-testing-30-câu-hỏi--bằng-chứng)
3. [Phần 3: L3 — System / API Testing (30 Câu hỏi & Bằng chứng)](#phần-3-l3--system--api-testing-30-câu-hỏi--bằng-chứng)
4. [Phần 4: L4 — End-to-End (E2E) Testing (30 Câu hỏi & Bằng chứng)](#phần-4-l4--end-to-end-e2e-testing-30-câu-hỏi--bằng-chứng)
5. [Phần 5: L5 / UAT — User Acceptance Testing (30 Câu hỏi & Bằng chứng)](#phần-5-l5--uat--user-acceptance-testing-30-câu-hỏi--bằng-chứng)
6. [Phần 6: Master Source Code Index — Danh mục toàn bộ file mã nguồn kiểm thử](#phần-6-master-source-code-index--danh-mục-toàn-bộ-file-mã-nguồn-kiểm-thử)

---

# PHẦN 1: L1 — UNIT TESTING (30 CÂU HỎI & BẰNG CHỨNG)

### 🟢 Mức độ Dễ (Câu 1 – 10: Khái niệm, Thiết lập & Vận hành cơ bản)

#### Câu 1: Em hãy giải thích Unit Test trong dự án ELog là gì và phạm vi của nó dừng lại ở đâu?
* **Hội đồng hỏi**: *"Em định nghĩa Unit Test trong backend Spring Boot của em như thế nào? Nó có chạm vào Database hay mạng bên ngoài không?"*
* **Trả lời phản biện**:
  * Unit Test (L1) trong ELog là việc kiểm thử các đơn vị logic nghiệp vụ nhỏ nhất (các hàm, phương thức trong tầng `Service`, `Util`, `Mapper`, `StateMachine`) trong trạng thái cô lập hoàn toàn.
  * **Phạm vi**: Chỉ kiểm thử code Java nội tại của class được test. Toàn bộ các phụ thuộc bên ngoài như Database (`Repository`), API bên ngoài (Goong Maps API), hoặc các Service khác đều được thay thế bằng Mock Objects thông qua thư viện **Mockito**.
* **Bằng chứng (Evidence)**:
  * File code: [AuthServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/AuthServiceImplTest.java) dùng `@ExtendWith(MockitoExtension.class)`, `@Mock private UserRepository userRepository;`, `@InjectMocks private AuthServiceImpl authService;`.

#### Câu 2: Dự án của em có chính xác bao nhiêu test case L1 Unit Test và phân bổ như thế nào?
* **Hội đồng hỏi**: *"Số lượng test case Unit test trong báo cáo của em là bao nhiêu? Em có bằng chứng gì chứng minh con số này không phải là số ảo?"*
* **Trả lời phản biện**:
  * Dự án có chính xác **363 Unit Test Cases** phân bổ trên **23 domain sheets** tương ứng với 23 Service logic của hệ thống.
  * Không có số ảo vì mỗi test case đều được ánh xạ 1:1 trong [catalog/l1.json](file:///d:/FULearning/semester%209/Elog/ELog-BE/test-execution/catalog/l1.json) và [Report 5.1_ELog_L1-UnitTests.xlsx](file:///d:/FULearning/semester%209/Elog/Report5/2026-08-16-Report5/Report%205.1_ELog_L1-UnitTests.xlsx) với ID duy nhất (`L1-AUT-01..12`, `L1-IMP-01..17`, `L1-TDS-01..34`, `L1-TRS-01..33`, `L1-CAP-01..27`, `L1-CON-01..09`, `L1-REC-01..21`, `L1-DTS-01..35`, `L1-TMS-01..21`, `L1-EXC-01..19`, `L1-ROU-01..19`, `L1-DIS-01..08`, `L1-MAN-01..10`, `L1-KPI-01..09`, `L1-VEH-01..13`, `L1-DSS-01..10`, `L1-STO-01..09`, `L1-PRO-01..11`, `L1-USE-01..15`, `L1-ROL-01..08`, `L1-TSM-01..07`, `L1-TOS-01..09`, `L1-MAP-01..07`).
* **Bằng chứng (Evidence)**:
  * File kết quả: [results/l1.json](file:///d:/FULearning/semester%209/Elog/ELog-BE/test-execution/results/l1.json) ghi nhận `summary.total = 363`, `summary.pass = 363`.
  * Script xác thực: `node test-execution/scripts/validate-results.mjs` trả về `PASS: 643 unique auditable results`.

#### Câu 3: Em sử dụng cấu trúc viết Unit Test như thế nào để người khác dễ đọc?
* **Hội đồng hỏi**: *"Một hàm test chuẩn của nhóm em gồm những phần nào?"*
* **Trả lời phản biện**:
  * Toàn bộ Unit test tuân thủ chặt chẽ chuẩn **BDD (Behavior-Driven Development) / AAA (Arrange - Act - Assert)**:
    1. **Given / Arrange**: Khởi tạo mock data, giả lập hành vi (`when(...).thenReturn(...)`).
    2. **When / Act**: Gọi phương thức cần kiểm thử.
    3. **Then / Assert**: Kiểm tra kết quả trả về (`assertEquals`, `assertTrue`), kiểm tra exception (`assertThrows`), và xác thực số lần gọi mock (`verify(..., times(1))`).
* **Bằng chứng (Evidence)**:
  * Phương thức `[L1-AUT-01] login_Success_ReturnsJwtToken` trong [AuthServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/AuthServiceImplTest.java#L30-L50).

#### Câu 4: Làm sao để chạy riêng toàn bộ Unit Test và xuất báo cáo?
* **Hội đồng hỏi**: *"Dùng lệnh Maven nào để chạy chỉ Unit test mà không chạy Integration test để tiết kiệm thời gian?"*
* **Trả lời phản biện**:
  * Chạy lệnh: `mvn test -Dtest=com.elog.service.**.*Test`
  * Maven Surefire Plugin sẽ thực thi toàn bộ các class test trong package `com.elog.service` và xuất báo cáo XML ra thư mục `ELog-BE/target/surefire-reports/TEST-com.elog.service.*.xml`.
* **Bằng chứng (Evidence)**:
  * 32 file XML báo cáo hiện hữu tại `ELog-BE/target/surefire-reports/`.

#### Câu 5: Công cụ nào đo độ bao phủ (Code Coverage) và chỉ số đạt được là bao nhiêu?
* **Hội đồng hỏi**: *"Nhóm dùng plugin gì đo coverage? Độ bao phủ tầng Service đạt bao nhiêu %?"*
* **Trả lời phản biện**:
  * Dự án tích hợp **JaCoCo (Java Code Coverage)** plugin phiên bản `0.8.12` trong `pom.xml`.
  * Báo cáo sinh ra tại `target/site/jacoco/jacoco.xml` và giao diện HTML `target/site/jacoco/index.html`.
  * Độ bao phủ Instruction Coverage tầng Service đạt **trên 85%**, các service cốt lõi như `CapacityValidationService`, `TripStateMachine` đạt **trên 92%**.
* **Bằng chứng (Evidence)**:
  * Cấu hình trong [pom.xml](file:///d:/FULearning/semester%209/Elog/ELog-BE/pom.xml#L110-L130) cấu hình `jacoco-maven-plugin`.

#### Câu 6: Phân biệt `@Mock` và `@InjectMocks` trong Mockito?
* **Hội đồng hỏi**: *"Nếu đặt `@Mock` vào `AuthServiceImpl` và `@InjectMocks` vào `UserRepository` thì code có chạy được không?"*
* **Trả lời phản biện**:
  * Không chạy được. `@Mock` dùng để tạo instance giả lập (mock object) cho các dependency (như `UserRepository`, `JwtTokenProvider`, `PasswordEncoder`), còn `@InjectMocks` tạo instance thật của class cần test (`AuthServiceImpl`) và tự động tiêm (inject) các `@Mock` vào constructor hoặc field của nó.

#### Câu 7: `@DisplayName` có tác dụng gì trong việc đối chiếu báo cáo kiểm thử?
* **Hội đồng hỏi**: *"Tại sao các method test lại có `@DisplayName("[L1-AUT-01] login_Success...")`?"*
* **Trả lời phản biện**:
  * Giúp gắn mã định danh Test ID chuẩn hóa quốc tế vào báo cáo Surefire XML. Nhờ đó, script kiểm toán `validate-results.mjs` có thể quét trực tiếp file XML để verify xem test case `L1-AUT-01` có thực sự được thực thi và Pass hay không.

#### Câu 8: `assertThrows` trong JUnit 5 hoạt động như thế nào?
* **Hội đồng hỏi**: *"Làm sao để test một hàm chắc chắn phải ném ra `AppException` khi mật khẩu sai?"*
* **Trả lời phản biện**:
  * Sử dụng `AppException ex = assertThrows(AppException.class, () -> authService.login(request));` sau đó `assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());`.

#### Câu 9: Tại sao Unit Test không nên dùng `@SpringBootTest`?
* **Hội đồng hỏi**: *"Tại sao em không dùng `@SpringBootTest` cho toàn bộ Unit test cho tiện?"*
* **Trả lời phản biện**:
  * Vì `@SpringBootTest` phải khởi động toàn bộ Spring ApplicationContext, tải DataSource, Hibernate, Component Scan... mất từ 5 - 15 giây mỗi class. Với 363 case, thời gian chạy sẽ lên tới hàng chục phút thay vì chỉ mất **2.8 giây** khi dùng Mockito `@ExtendWith(MockitoExtension.class)`.

#### Câu 10: `verify(mock, times(1))` dùng để kiểm tra điều gì?
* **Hội đồng hỏi**: *"Nếu hàm của em trả về đúng nhưng quên gọi `repository.save()` thì test có bắt được không?"*
* **Trả lời phản biện**:
  * Có bắt được nhờ `verify(tripRepository, times(1)).save(any(Trip.class));`. Nếu nghiệp vụ không gọi hàm `save` hoặc gọi 2 lần thì `verify` sẽ ném ra `AssertionError`.

---

### 🟡 Mức độ Trung bình (Câu 11 – 20: Kỹ thuật kiểm thử & Nghiệp vụ đặc thù)

#### Câu 11: Nhóm áp dụng kỹ thuật Phân tích giá trị biên (BVA) như thế nào trong kiểm thử tải trọng xe?
* **Hội đồng hỏi**: *"Xe có tải trọng tối đa 2000kg. Em thiết kế các ca kiểm thử biên như thế nào?"*
* **Trả lời phản biện**:
  * Áp dụng Boundary Value Analysis với tải trọng xe $W_{max} = 2000$ kg và Safety Buffer = 95% (tức ngưỡng thực tế là 1900kg):
    1. Dưới biên (Valid): 1899 kg ➔ Pass (`L1-CAP-03`).
    2. Tại biên (On-point): 1900 kg ➔ Pass (`L1-CAP-01`).
    3. Vượt biên 1 đơn vị (Invalid): 1901 kg ➔ Fail / Rejection (`L1-CAP-04`).
    4. Vượt xa biên: 2500 kg ➔ Fail (`L1-CAP-05`).
* **Bằng chứng (Evidence)**:
  * File test: [CapacityValidationServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/CapacityValidationServiceImplTest.java#L80-L120).

#### Câu 12: Kỹ thuật Phân vùng tương đương (Equivalence Partitioning - EP) áp dụng ở module Import Excel ra sao?
* **Hội đồng hỏi**: *"Hàm import Excel xử lý cột 'Số lượng hàng' như thế nào qua các phân vùng hợp lệ và không hợp lệ?"*
* **Trả lời phản biện**:
  * Chia làm 3 phân vùng tương đương:
    * Phân vùng hợp lệ: Số nguyên dương $\ge 1$ (VD: `5`, `100`) ➔ Chấp nhận (`L1-IMP-01`).
    * Phân vùng không hợp lệ 1: Số $\le 0$ hoặc số thập phân (VD: `0`, `-3`, `2.5`) ➔ Báo lỗi từng dòng (`L1-IMP-03`, `L1-IMP-22`).
    * Phân vùng không hợp lệ 2: Ký tự chữ hoặc để trống (VD: `"abc"`, `null`) ➔ Ghi nhận vào danh sách `rejectedRows` (`L1-IMP-02`).
* **Bằng chứng (Evidence)**:
  * File test: [ImportServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/ImportServiceImplTest.java#L50-L95).

#### Câu 13: Làm thế nào để kiểm thử thuật toán tính khoảng cách và ETA Haversine?
* **Hội đồng hỏi**: *"Khoảng cách giữa 2 tọa độ địa lý được kiểm thử độ chính xác ra sao?"*
* **Trả lời phản biện**:
  * Kiểm thử qua 4 ca kiểm thử mẫu chuẩn địa lý:
    1. Tọa độ trùng nhau: Cùng 1 điểm $(lat_1=lat_2, lng_1=lng_2) \implies Distance = 0.0$ km (`L1-MAP-01`).
    2. Tuyến Hà Nội – TP.HCM: Tọa độ $(21.0285, 105.8542)$ đến $(10.8231, 106.6297) \implies 1138 \pm 20$ km (`L1-MAP-02`).
    3. Hai cực đối xứng qua tâm trái đất (Antipodal poles) $\implies \approx 20,015$ km.
    4. Hai điểm nội thành khoảng cách ngắn 2km với tốc độ xe tải trung bình $30$ km/h $\implies ETA = 4$ phút.
* **Bằng chứng (Evidence)**:
  * File test: [HaversineEtaCalculatorTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/impl/HaversineEtaCalculatorTest.java#L20-L60).

#### Câu 14: State Machine của vòng đời chuyến đi (`TripStateMachine`) được Unit test như thế nào?
* **Hội đồng hỏi**: *"Làm sao đảm bảo một chuyến xe đã 'COMPLETED' không bao giờ bị chuyển ngược về 'IN_PROGRESS' hoặc 'VALIDATED'?"*
* **Trả lời phản biện**:
  * Kiểm thử ma trận chuyển trạng thái (State Transition Matrix):
    * Chuyển đổi hợp lệ: `DRAFT ➔ VALIDATED ➔ DISPATCHED ➔ IN_PROGRESS ➔ COMPLETED` (`L1-TSM-01..03`).
    * Chuyển đổi bất hợp lệ: Thử chuyển từ `COMPLETED` sang bất kỳ trạng thái nào khác $\implies$ Ném `IllegalStateException` hoặc `AppException(INVALID_STATE_TRANSITION)` (`L1-TSM-05`).
* **Bằng chứng (Evidence)**:
  * File test: [TripStateMachineTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/TripStateMachineTest.java#L30-L75).

#### Câu 15: Kiểm thử gom đơn tự động (Auto Pipeline Consolidation) cần mock những gì?
* **Hội đồng hỏi**: *"Khi gom đơn theo ngày giao hàng, nếu có 1 đơn hàng thuộc cửa hàng chưa gắn tuyến thì xử lý ra sao?"*
* **Trả lời phản biện**:
  * Mock `OrderRepository.findUnassignedOrdersByDate(date)` trả về danh sách đơn. Mock `StoreRouteRepository` để tìm tuyến.
  * Nếu cửa hàng chưa có tuyến, thuật toán phải bỏ qua đơn đó (`unmappedOrders`), không gây crash chương trình và ghi log cảnh báo (`L1-TDS-06`).
* **Bằng chứng (Evidence)**:
  * Method `consolidate_unmappedOrders_skipped` trong [TripDraftServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/TripDraftServiceImplTest.java).

#### Câu 16: Kiểm thử logic sinh bảng kê xếp dỡ ngược chiều LIFO (Last-In First-Out) như thế nào?
* **Hội đồng hỏi**: *"Nguyên tắc xếp hàng lên xe là gì và Unit test assert điều này như thế nào?"*
* **Trả lời phản biện**:
  * Điểm giao hàng đầu tiên (Stop 1) phải được bốc dỡ đầu tiên, do đó hàng của Stop 1 phải được xếp vào thùng xe **sau cùng (Last-In)**. Hàng của điểm cuối (Stop N) phải xếp vào **đầu tiên (First-In)**.
  * Test case `L1-MAN-01` assert thứ tự `loadingSequence`: Stop cuối có `loadingSequence = 1`, Stop đầu tiên có `loadingSequence = N`.
* **Bằng chứng (Evidence)**:
  * File test: [ManifestServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/ManifestServiceImplTest.java).

#### Câu 17: Làm sao Unit test được trường hợp token JWT hết hạn hoặc chữ ký bị giả mạo?
* **Hội đồng hỏi**: *"Làm sao test `JwtTokenProvider` với token hết hạn mà không cần phải `Thread.sleep` chờ thật?"*
* **Trả lời phản biện**:
  * Khởi tạo `JwtTokenProvider` với cấu hình thời gian hết hạn âm (VD: `-1000` ms) hoặc mock hàm `extractClaims` ném ra `ExpiredJwtException` / `SignatureException`, sau đó assert `validateToken(expiredToken)` trả về `false` (`L1-AUT-09`).

#### Câu 18: Unit test xử lý trường hợp tài xế từ chối đơn hàng (Delivery Rejection) kiểm tra những gì?
* **Hội đồng hỏi**: *"Khi tài xế báo từ chối giao hàng tại 1 điểm, dữ liệu nào phải thay đổi?"*
* **Trả lời phản biện**:
  * Kiểm tra 3 điều kiện:
    1. Trạng thái `TripStop` chuyển thành `REJECTED` (`L1-DTS-12`).
    2. Bắt buộc phải có `rejectionReason` không được rỗng (`L1-DTS-13`).
    3. Tạo bản ghi trong bảng `delivery_exceptions` với mức độ ưu tiên tương ứng (`L1-EXC-02`).

#### Câu 19: Làm sao để kiểm thử hàm tính KPI OTD (On-Time Delivery Rate)?
* **Hội đồng hỏi**: *"Công thức OTD là gì và test case kiểm thử độ chính xác ra sao?"*
* **Trả lời phản biện**:
  * $OTD = \frac{\text{Số đơn giao đúng giờ}}{\text{Tổng số đơn hoàn thành}} \times 100\%$.
  * Mock 8 đơn giao trước ETA và 2 đơn giao trễ hơn ETA $\implies$ Assert `otdRate = 80.0%` (`L1-KPI-01`).
* **Bằng chứng (Evidence)**:
  * File test: [KpiServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/KpiServiceImplTest.java).

#### Câu 20: Tại sao phải kiểm thử việc tính toán sai số làm tròn số thực `BigDecimal` trong tính tổng thể tích?
* **Hội đồng hỏi**: *"Tại sao trong thương mại logistics không dùng kiểu `double` mà dùng `BigDecimal`?"*
* **Trả lời phản biện**:
  * Kiểu `double` có sai số dấu phẩy động IEEE 754 (VD: $0.1 + 0.2 = 0.30000000000000004$). Khi cộng dồn hàng nghìn kiện hàng nhỏ, sai số này sẽ làm sai lệch kiểm tra tải trọng xe.
  * Unit test kiểm tra phép cộng `BigDecimal` với scale 4 và làm tròn `RoundingMode.HALF_UP` (`L1-CAP-15`).

---

### 🔴 Mức độ Khó / Phản biện cao (Câu 21 – 30: Kiến trúc, Thuật toán & Tối ưu)

#### Câu 21: Thuật toán gợi ý ghép 2 xe (Two-Vehicle Fallback Recommendation) hoạt động và được kiểm thử như thế nào?
* **Hội đồng hỏi**: *"Nếu một tuyến hàng có tổng khối lượng 3500kg mà đội xe chỉ có các xe 2000kg và 2500kg (không xe đơn nào chở hết), thuật toán của em giải quyết thế nào?"*
* **Trả lời phản biện**:
  * `RecommendationService` kích hoạt cơ chế Fallback: Tìm cặp 2 xe $(V_1, V_2)$ có tổng tải trọng $W(V_1) + W(V_2) \ge W_{total}$ và tối ưu hóa chi phí quãng đường / chi phí rỗng.
  * Test case `L1-REC-10` giả lập không có xe đơn nào vừa, assert thuật toán trả về danh sách gợi ý gồm 2 xe hợp lệ kèm tỷ lệ phân bổ tải trọng an toàn.
* **Bằng chứng (Evidence)**:
  * File test: [RecommendationServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/RecommendationServiceImplTest.java#L110-L150).

#### Câu 22: Làm sao phát hiện Mockito bị "State Leakage" giữa các test methods?
* **Hội đồng hỏi**: *"Nếu một test method cấu hình `when(repo.findById(1L)).thenReturn(tripA)` mà method sau lại cần `tripB` thì có bị ảnh hưởng không?"*
* **Trả lời phản biện**:
  * JUnit 5 khởi tạo một instance mới của class test cho mỗi test method (mặc định `@TestInstance(Lifecycle.PER_METHOD)`), kết hợp với `MockitoExtension` tự động reset mocks trước mỗi lần chạy test method, loại bỏ hoàn toàn rủi ro ô nhiễm trạng thái (State Pollution).

#### Câu 23: Làm sao Unit test được logic điều chỉnh giờ xuất bến và tính lại toàn bộ chuỗi ETA (`DepartureAdjustmentService`)?
* **Hội đồng hỏi**: *"Nếu điều phối viên dời giờ xuất bến từ 08:00 sang 08:45, các điểm dừng tiếp theo bị ảnh hưởng thế nào và test assert gì?"*
* **Trả lời phản biện**:
  * Dời giờ xuất bến thêm +45 phút sẽ làm tịnh tiến toàn bộ chuỗi ETA của các Stop $S_1, S_2, ..., S_n$ thêm đúng 45 phút nếu thời gian di chuyển giữa các chặng không đổi.
  * Test case `L1-TRS-25` kiểm tra:
    1. Giờ xuất bến mới được cập nhật thành `08:45:00`.
    2. Stop 1 ETA cập nhật từ `08:30:00` thành `09:15:00`.
    3. Nếu Stop 2 có khung giờ đóng cửa là `09:00:00`, hệ thống phải cảnh báo vi phạm khung giờ giao (`TimeWindowViolationException`).

#### Câu 24: Làm thế nào để kiểm thử logic loại trừ đơn hàng trễ (`excludeOrder`) mà không làm hỏng tính toàn vẹn của chuyến gom?
* **Hội đồng hỏi**: *"Khi loại 1 đơn hàng ra khỏi Trip Draft, những chỉ số nào bắt buộc phải tính toán lại?"*
* **Trả lời phản biện**:
  * Khi loại trừ đơn hàng:
    1. Tổng tải trọng `totalWeightKg` và thể tích `totalVolumeM3` của chuyến gom phải giảm đi đúng khối lượng đơn bị loại.
    2. Số đơn tại điểm dừng (`orderCount`) giảm đi 1.
    3. Nếu điểm dừng đó không còn đơn nào khác, trạng thái điểm dừng chuyển thành `isActive = false` và `skippedStopCount` tăng lên 1 (`L1-TDS-18`).

#### Câu 25: Phản biện: "Nếu toàn bộ Repository đều bị Mock, làm sao em chắc chắn câu lệnh SQL thực tế chạy đúng?"
* **Hội đồng hỏi**: *"Unit test của em 100% mock repository, vậy lỡ câu query trong Repository viết sai cú pháp SQL thì Unit test có phát hiện được không?"*
* **Trả lời phản biện**:
  * **Hội đồng nhận xét rất chính xác**: Đây chính là giới hạn của Unit Test (L1) — nó chỉ kiểm tra logic điều hướng và biến đổi dữ liệu của Service chứ không kiểm tra câu lệnh SQL hay tương tác cơ sở dữ liệu thật.
  * Để khắc phục điều này, dự án đã xây dựng tầng **L2 (Integration Test)** gồm **75 test cases** chạy trực tiếp trên cơ sở dữ liệu MySQL thật để kiểm tra toàn bộ câu query JPA/Hibernate và ràng buộc khóa ngoại.

#### Câu 26: Kiểm thử phân quyền tài xế lái xe phù hợp với hạng giấy phép lái xe (Driver License Compatibility) được thực hiện thế nào?
* **Hội đồng hỏi**: *"Quy tắc nghiệp vụ nào quy định tài xế bằng B2 không được lái xe tải 5 tấn và test case nào kiểm tra?"*
* **Trả lời phản biện**:
  * Nghiệp vụ BR-DRV-01: Bằng B2 chỉ lái được xe $\le 3.5$ tấn. Xe 5 tấn yêu cầu bằng hạng C trở lên.
  * Test case `L1-TRS-08` gán tài xế có bằng B2 cho xe tải 5 tấn $\implies$ Ném ra `AppException(INCOMPATIBLE_DRIVER_LICENSE)`.
* **Bằng chứng (Evidence)**:
  * File test: [TripServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/TripServiceImplTest.java).

#### Câu 27: Làm sao Unit test được cơ chế phát hiện ngoại lệ thời gian tự động (`TimeExceptionDetectionJob`)?
* **Hội đồng hỏi**: *"Một chuyến xe đang chạy, tài xế chưa bấm 'Đã đến điểm' mà đồng hồ hệ thống đã quá ETA 30 phút thì hệ thống test thế nào?"*
* **Trả lời phản biện**:
  * Mock `Clock` hệ thống tại thời điểm $T = 10:30$. Mock Stop có `plannedEta = 10:00` và `status = PENDING`.
  * Job quét phát hiện độ trễ vượt quá ngưỡng dung sai (`delayThresholdMinutes = 15`) $\implies$ Tự động tạo ticket ngoại lệ `DELAY_STOP` gắn vào Dashboard điều phối (`L1-EXC-15`).

#### Câu 28: Kiểm thử nghiệp vụ sửa đổi kết quả chuyến đi sau kiểm toán (`TripOutcomeService.amendOutcome`) đòi hỏi điều kiện bắt buộc gì?
* **Hội đồng hỏi**: *"Khi quản trị viên sửa số tiền thu hộ (COD) hoặc số hàng giao thành công, test case kiểm tra điều gì để chống gian lận?"*
* **Trả lời phản biện**:
  * Bắt buộc phải có lý do điều chỉnh (`amendmentReason`) tối thiểu 10 ký tự và ghi nhận `amendedBy` là User ID của người thực hiện.
  * Test case `L1-TOS-03` thử gửi request không có `reason` $\implies$ Ném ra `ValidationException` và không cho phép cập nhật DB.

#### Câu 29: Unit test có phát hiện được lỗi tràn số nguyên (Integer Overflow) khi tính tải trọng không?
* **Hội đồng hỏi**: *"Nếu tải trọng tính bằng gram hoặc miligram và bị tràn kiểu `int`, Unit test thiết kế thế nào?"*
* **Trả lời phản biện**:
  * Tất cả các trường khối lượng và thể tích trong Entity và DTO đều sử dụng kiểu `Double` hoặc `BigDecimal` / `Long`. Test case `L1-CAP-25` truyền giá trị cực lớn ($10^9$) để kiểm tra không bị lỗi tràn số hoặc ném exception không mong muốn.

#### Câu 30: Tổng kết: Tại sao nhóm không giữ 411 case mà chuẩn hóa về đúng 363 case?
* **Hội đồng hỏi**: *"Hồi nãy thầy thấy trong log có 411 test case, sao giờ lại chốt 363 case? Có phải nhóm xóa bớt test không?"*
* **Trả lời phản biện**:
  * Không phải xóa bớt test. Con số **411** là tổng số test tích lũy trong thư mục `target/surefire-reports` bao gồm các test case edge case chạy thử nghiệm trước đó.
  * Con số **363 test cases** là bộ test tiêu chuẩn chính thức được chuẩn hóa, phân loại rõ ràng 1:1 theo đúng 23 Domain Service Sheets trong tài liệu [Report 5.1_ELog_L1-UnitTests.xlsx](file:///d:/FULearning/semester%209/Elog/Report5/2026-08-16-Report5/Report%205.1_ELog_L1-UnitTests.xlsx). Việc chuẩn hóa giúp báo cáo minh bạch, không trùng lặp và truy vết chính xác tuyệt đối.

---

# PHẦN 2: L2 — INTEGRATION TESTING (30 CÂU HỎI & BẰNG CHỨNG)

### 🟢 Mức độ Dễ (Câu 31 – 40: Môi trường, Flyway & `@SpringBootTest`)

#### Câu 31: Integration Test (L2) trong dự án ELog khác gì so với Unit Test (L1)?
* **Trả lời phản biện**:
  * L1 chỉ test logic cô lập bằng Mockito. L2 khởi động ngữ cảnh Spring Boot thật (`@SpringBootTest`), kết nối tới hệ quản trị cơ sở dữ liệu **MySQL 9.7 thật** trên cổng `3307`, thực thi các câu lệnh SQL thật để kiểm tra tương tác giữa Repository, Hibernate ORM và Database.

#### Câu 32: Dự án có bao nhiêu test case Integration Test và kết quả ra sao?
* **Trả lời phản biện**:
  * Dự án có **75 Integration Test Cases** trong catalog `catalog/l2.json`, kết quả thực thi đạt **75/75 Pass (100%)**.

#### Câu 33: Flyway đóng vai trò gì trong Integration Testing?
* **Trả lời phản biện**:
  * Flyway tự động thực thi 58 tệp SQL migration (`V1__init_schema.sql` đến `V58__...`) để khởi tạo schema bảng, ràng buộc toàn vẹn, view, index và dữ liệu mẫu chuẩn (seed data) trước khi test chạy.

#### Câu 34: Annotation `@Transactional` trên class test có ý nghĩa gì?
* **Trả lời phản biện**:
  * Mặc định trong Spring Test, `@Transactional` sẽ tự động **Rollback (hoàn tác)** mọi thay đổi dữ liệu (INSERT, UPDATE, DELETE) sau khi mỗi test method kết thúc, giữ cho Database luôn sạch sẽ cho test tiếp theo.

#### Câu 35: Làm sao để chạy riêng bộ Integration Test?
* **Trả lời phản biện**:
  * Chạy lệnh: `mvn test -Dtest=com.elog.integration.**.*Test`

#### Câu 36: Database dùng cho Integration test là H2 in-memory hay MySQL thật?
* **Trả lời phản biện**:
  * Dùng **MySQL 9.7 thật** chạy trên port 3307 để đảm bảo tương thích 100% các hàm ngày giờ (`DATE_ADD`, `TIMEDIFF`), kiểu dữ liệu JSON, và cơ chế khóa của MySQL mà H2 không mô phỏng chính xác được.

#### Câu 37: `TestEntityManager` trong Spring Boot Test dùng để làm gì?
* **Trả lời phản biện**:
  * Dùng để hỗ trợ thao tác trực tiếp với JPA Persistence Context (như `persistAndFlush`, `clear`, `find`) mà không cần thông qua tầng Repository.

#### Câu 38: Kiểm thử quan hệ `@OneToMany` và `@ManyToOne` giữa `Trip` và `TripStop` như thế nào?
* **Trả lời phản biện**:
  * Tạo 1 `Trip`, thêm 3 `TripStop`, gọi `tripRepository.save(trip)`. Dùng `entityManager.clear()` xóa cache Hibernate rồi `tripRepository.findById(id)` để kiểm tra 3 Stop có được lưu và fetch đúng không (`L2-TRP-01`).

#### Câu 39: `@DirtiesContext` được sử dụng khi nào trong Integration Test?
* **Trả lời phản biện**:
  * Dùng khi một test method làm thay đổi cấu hình context của Spring (như thay đổi cache, mock bean) để yêu cầu Spring khởi tạo lại context mới cho class tiếp theo.

#### Câu 40: Làm sao kiểm tra ràng buộc Unique Constraint của mã đơn hàng (`order_code`) trong Database?
* **Trả lời phản biện**:
  * Insert 2 bản ghi `Order` có cùng `order_code = 'DH-001'` $\implies$ Assert ném ra `DataIntegrityViolationException` (`L2-ORD-05`).

---

### 🟡 Mức độ Trung bình (Câu 41 – 50: Giao dịch, Khóa ngoại & Khối lượng dữ liệu)

#### Câu 41: Kiểm thử hành vi Xóa theo tầng (Cascade Delete) đối với `DeliveryOrderResult`?
* **Trả lời phản biện**:
  * Bản migration `V50__add_on_delete_cascade_to_delivery_order_results.sql` cấu hình cascade. Test case `L2-OUT-04` xóa 1 `TripStop` $\implies$ Kiểm tra toàn bộ `DeliveryOrderResult` liên quan trong DB tự động bị xóa theo, không sinh lỗi orphan foreign key.

#### Câu 42: Kiểm thử câu query phức tạp tìm xe khả dụng (`findAvailableVehiclesForDate`) trong DB?
* **Trả lời phản biện**:
  * Tạo 3 xe trong DB: Xe 1 đang ở chuyến `DISPATCHED` vào ngày X, Xe 2 đang bảo dưỡng (`MAINTENANCE`), Xe 3 rảnh (`AVAILABLE`). Chạy hàm query $\implies$ Assert chỉ có Xe 3 được trả về trong kết quả (`L2-VEH-03`).

#### Câu 43: Kiểm thử lưu trữ tọa độ địa lý và Ward Code chuẩn hành chính Việt Nam (Flyway V19 & V27)?
* **Trả lời phản biện**:
  * Lưu Store với `ward_code = '00001'` (Ba Đình, Hà Nội). Query từ Database assert đúng định dạng chuỗi và quan hệ bảng đơn vị hành chính 3 cấp (`L2-STO-02`).

#### Câu 44: Kiểm thử bảng kê xếp hàng LIFO với dữ liệu thật trong MySQL?
* **Trả lời phản biện**:
  * Tạo Trip với 5 Stop trong DB. Gọi `manifestService.generateManifest(tripId)` $\implies$ Kiểm tra bản ghi trong bảng `manifests` và `manifest_items` có thứ tự ngược chính xác từ 5 về 1 (`L2-MAN-01`).

#### Câu 45: Kiểm thử phân trang (Pagination) và sắp xếp (Sorting) ở danh sách đơn hàng?
* **Trả lời phản biện**:
  * Seed 50 đơn hàng vào DB. Gọi `orderRepository.findAll(PageRequest.of(0, 10, Sort.by("createdAt").descending()))` $\implies$ Assert trả về đúng 10 bản ghi mới nhất và `totalElements = 50` (`L2-ORD-08`).

#### Câu 46: Kiểm thử trường hợp lưu trữ Polyline tuyến đường dài (`V52__alter_route_polyline_longtext.sql`)?
* **Trả lời phản biện**:
  * Chuỗi polyline mã hóa thuật toán Google/Goong cho tuyến 30 điểm dừng có độ dài > 10.000 ký tự. Test case lưu chuỗi này vào cột `LONGTEXT` $\implies$ Query ra kiểm tra nguyên vẹn không bị cắt cụt dữ liệu (`L2-ROU-04`).

#### Câu 47: Kiểm thử tính năng cập nhật trạng thái hoạt động của tài xế (`DriverActiveStatus`)?
* **Trả lời phản biện**:
  * Chèn bản ghi tài xế `OFF_DUTY`. Cập nhật sang `AVAILABLE`. Query trực tiếp bảng `driver_active_status` kiểm tra cột `updated_at` được tự động cập nhật timestamp hiện tại (`L2-DSS-01`).

#### Câu 48: Kiểm thử Transactional Rollback khi xảy ra lỗi giữa chừng trong gom đơn?
* **Trả lời phản biện**:
  * Trong quá trình gom 10 đơn vào chuyến, đơn số 9 bị lỗi vi phạm ràng buộc $\implies$ Toàn bộ giao dịch rollback, bảng `trip_drafts` không được tạo rác dữ liệu (`L2-TDS-08`).

#### Câu 49: Kiểm thử lưu vết lịch sử lập kế hoạch (`PlanningHistory` - V39)?
* **Trả lời phản biện**:
  * Khi Dispatcher thực hiện split chuyến, test case kiểm tra bảng `planning_history` có thêm bản ghi ghi nhận `action = 'SPLIT_DRAFT'`, `actor_id`, `before_state`, `after_state` dưới dạng JSON (`L2-PLN-01`).

#### Câu 50: Kiểm thử bảng `refresh_tokens` và cơ chế thu hồi token trong DB?
* **Trả lời phản biện**:
  * Tạo Refresh Token trong DB với `expiryDate`. Gọi hàm thu hồi (revoke) $\implies$ Assert cột `revoked = true`, cố tình dùng lại token này $\implies$ Báo lỗi xác thực (`L2-AUT-04`).

---

### 🔴 Mức độ Khó / Phản biện cao (Câu 51 – 60: Concurrency, Optimistic Locking & Deadlock)

#### Câu 51: Làm sao kiểm thử Khóa lạc quan (Optimistic Locking) trên `TripDraft` để chống xung đột ghi đè?
* **Hội đồng hỏi**: *"Nếu 2 Dispatcher cùng mở 1 Trip Draft và bấm điều chỉnh departure time cùng 1 lúc, hệ thống xử lý thế nào?"*
* **Trả lời phản biện**:
  * Entity `TripDraft` có trường `@Version private Long version;`.
  * Integration test giả lập 2 luồng:
    1. Luồng 1 đọc Draft (version 0).
    2. Luồng 2 đọc Draft (version 0).
    3. Luồng 1 cập nhật thành công $\implies$ Version tăng lên 1.
    4. Luồng 2 cố gắng cập nhật với version 0 cũ $\implies$ Hibernate ném ra `ObjectOptimisticLockingFailureException` (`L2-TDS-15`).

#### Câu 52: Kiểm thử hiện tượng Deadlock khi nhiều tài xế cùng cập nhật trạng thái Stop cùng lúc?
* **Trả lời phản biện**:
  * Sử dụng `ExecutorService` bắn 10 luồng cập nhật đồng thời vào các bản ghi `TripStop` khác nhau trong cùng 1 Trip. Thiết lập transaction isolation level `READ_COMMITTED` và đánh index trên khóa ngoại `trip_id` để tránh table lock gây deadlock.

#### Câu 53: Tại sao không dùng `@Transactional` trong test nếu muốn kiểm tra dữ liệu thật sự được commit xuống DB?
* **Trả lời phản biện**:
  * Nếu đặt `@Transactional` trên test method, Hibernate có thể trì hoãn việc flush SQL xuống DB. Khi cần test ràng buộc DB thật, bỏ `@Transactional`, gọi `repository.saveAndFlush()`, sau đó dùng `try-finally` tự cleanup dữ liệu đã chèn.

#### Câu 54: Kiểm thử cơ chế Decouple lịch sử sự kiện khỏi khóa ngoại (`V42__decouple_history_events`) nhằm mục đích gì?
* **Trả lời phản biện**:
  * Khi một đơn hàng hoặc chuyến xe cũ bị xóa hoặc lưu trữ, các bản ghi log sự kiện lịch sử trong `trip_outcome_history` không được phép bị mất. Test case kiểm tra xóa `Trip` nhưng bản ghi trong `trip_outcome_history` vẫn tồn tại nguyên vẹn.

#### Câu 55: Kiểm thử hiệu năng câu query tìm kiếm phân trang đơn hàng với Specification động?
* **Trả lời phản biện**:
  * Sử dụng `JpaSpecificationExecutor` với 5 tiêu chí lọc (mã đơn, cửa hàng, ngày giao, trạng thái, khoảng cân nặng). Test kiểm tra query sinh ra mệnh đề `WHERE` tối ưu có sử dụng Index trên `delivery_date` và `status`.

---

# PHẦN 3: L3 — SYSTEM / API TESTING (30 CÂU HỎI & BẰNG CHỨNG)

### 🟢 Mức độ Dễ (Câu 61 – 70: MockMvc, Endpoints & HTTP Contracts)

#### Câu 61: System/API Test (L3) kiểm thử tầng nào trong ứng dụng?
* **Trả lời phản biện**:
  * L3 kiểm thử tầng **REST Controller** thông qua công cụ **`MockMvc`** của Spring Test, kiểm tra toàn bộ luồng từ HTTP Request ➔ Filter bảo mật ➔ Controller ➔ Validation ➔ HTTP Response Code & Body JSON.

#### Câu 62: Dự án có bao nhiêu API Test Cases và tỷ lệ Pass là bao nhiêu?
* **Trả lời phản biện**:
  * Có **130 API Test Cases** trong catalog `catalog/l3.json`, kết quả đạt **130/130 Pass (100%)**.

#### Câu 63: Cấu trúc response chuẩn của API trong ELog là gì?
* **Trả lời phản biện**:
  * Luôn đóng gói trong cấu trúc `ApiResponse<T>`:
    ```json
    {
      "success": true,
      "code": 200,
      "message": "OK",
      "data": { ... }
    }
    ```
  * Khi có lỗi: `"success": false, "error": { "code": "ERROR_CODE", "message": "Chi tiết lỗi" }`.

#### Câu 64: Khi client gửi request thiếu trường bắt buộc thì API trả về HTTP code nào?
* **Trả lời phản biện**:
  * Trả về **HTTP 400 Bad Request** kèm danh sách các trường vi phạm validation từ `@Valid` (`L3-VAL-01`).

#### Câu 65: Khi client không đính kèm JWT token mà gọi API yêu cầu bảo mật thì nhận HTTP code nào?
* **Trả lời phản biện**:
  * Trả về **HTTP 401 Unauthorized** do `JwtAuthFilter` chặn lại (`L3-SEC-01`).

#### Câu 66: Khi Tài xế cố tình gọi API của Điều phối viên thì nhận HTTP code nào?
* **Trả lời phản biện**:
  * Trả về **HTTP 403 Forbidden** do `@PreAuthorize("hasRole('DISPATCHER')")` từ chối quyền hạn (`L3-SEC-03`).

#### Câu 67: Khi tạo mới một tài nguyên (VD: tạo Chuyến xe thành công) thì API trả về HTTP code nào?
* **Trả lời phản biện**:
  * Trả về **HTTP 201 Created** (`L3-TRP-01`).

#### Câu 68: Làm sao MockMvc giả lập một user đã đăng nhập với Role cụ thể?
* **Trả lời phản biện**:
  * Sử dụng annotation `@WithMockUser(username = "dispatcher01", roles = {"DISPATCHER"})`.

#### Câu 69: Kiểm thử API Upload file Excel `POST /api/v1/imports` dùng phương thức nào trong MockMvc?
* **Trả lời phản biện**:
  * Sử dụng `MockMvcRequestBuilders.multipart("/api/v1/imports").file(mockMultipartFile)`.

#### Câu 70: Làm sao kiểm tra nội dung JSON trả về bằng `jsonPath`?
* **Trả lời phản biện**:
  * `mockMvc.perform(...).andExpect(jsonPath("$.data.status").value("VALIDATED"))`.

---

### 🟡 Mức độ Trung bình (Câu 71 – 80: Validation, Security Filters & Complex Payloads)

#### Câu 71: Kiểm thử validate định dạng số điện thoại và email người nhận đơn hàng?
* **Trả lời phản biện**:
  * Gửi payload có `recipientPhone = "12345"` (sai chuẩn Việt Nam 10 số) $\implies$ Assert HTTP 400 và `jsonPath("$.error.details.recipientPhone")` tồn tại (`L3-ORD-03`).

#### Câu 72: Kiểm thử API xác nhận chuyến gom `POST /api/v1/trip-drafts/{id}/confirm`?
* **Trả lời phản biện**:
  * Mock service confirm thành công $\implies$ Assert HTTP 200, response trả về status `PLANNED` và thông tin các điểm dừng (`L3-TDS-05`).

#### Câu 73: Kiểm thử API phân xe và tài xế `POST /api/v1/trips/{id}/assign`?
* **Trả lời phản biện**:
  * Gửi `{ "vehicleId": 1, "driverId": 4 }` $\implies$ Assert HTTP 200, trạng thái chuyến chuyển thành `ASSIGNED` (`L3-TRP-04`).

#### Câu 74: Kiểm thử API cập nhật vị trí GPS tài xế `POST /api/v1/driver-trips/{id}/location`?
* **Trả lời phản biện**:
  * Gửi `{ "latitude": 10.7769, "longitude": 106.7009 }` $\implies$ Assert HTTP 200 và vị trí được ghi nhận vào hệ thống giám sát (`L3-DRV-08`).

#### Câu 75: Kiểm thử API từ chối nhận hàng `POST /api/v1/trip-stops/{id}/reject`?
* **Trả lời phản biện**:
  * Gửi `{ "rejectionReason": "Cửa hàng đóng cửa", "photoUrl": "https://..." }` $\implies$ Assert HTTP 200 (`L3-DRV-12`).

#### Câu 76: Kiểm thử Global Exception Handler bắt `EntityNotFoundException`?
* **Trả lời phản biện**:
  * Gọi `GET /api/v1/stores/99999` (ID không tồn tại) $\implies$ Global Exception Handler bắt lỗi và trả về **HTTP 404 Not Found** với mã lỗi `STORE_NOT_FOUND` (`L3-STO-04`).

#### Câu 77: Kiểm thử CORS (Cross-Origin Resource Sharing) filter?
* **Trả lời phản biện**:
  * Gửi request với Header `Origin: http://localhost:5173` $\implies$ Assert response có Header `Access-Control-Allow-Origin: http://localhost:5173` (`L3-SEC-10`).

#### Câu 78: Kiểm thử API lọc danh sách chuyến xe theo khoảng ngày?
* **Trả lời phản biện**:
  * `GET /api/v1/trips?startDate=2026-08-16&endDate=2026-08-18&status=IN_PROGRESS` $\implies$ Assert HTTP 200 và danh sách lọc chính xác (`L3-TRP-09`).

#### Câu 79: Kiểm thử API xuất file báo cáo lỗi import dưới dạng byte stream Excel?
* **Trả lời phản biện**:
  * `GET /api/v1/imports/101/error-report` $\implies$ Assert Header `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` và Content-Disposition `attachment; filename=...` (`L3-IMP-06`).

#### Câu 80: Kiểm thử Swagger UI OpenAPI docs endpoint?
* **Trả lời phản biện**:
  * `GET /api-docs` $\implies$ Assert HTTP 200 và body chứa định dạng OpenAPI 3.0 schema với đầy đủ các path API (`L3-DOC-01`).

---

### 🔴 Mức độ Khó / Phản biện cao (Câu 81 – 90: Bảo mật IDOR, Idempotency & Rate Limit)

#### Câu 81: Kiểm thử lỗ hổng IDOR (Insecure Direct Object Reference) ở API tài xế?
* **Hội đồng hỏi**: *"Tài xế A đang đăng nhập, nếu gửi request xem thông tin chuyến đi của Tài xế B (`GET /api/v1/driver-trips/chuyen-cua-tai-xe-B`) thì API xử lý thế nào?"*
* **Trả lời phản biện**:
  * Hệ thống kiểm tra ID tài xế trong JWT Token (`principal.getUserId()`) với `assignedDriverId` của chuyến đi.
  * Nếu không khớp $\implies$ Ném ra `AccessDeniedException` và trả về **HTTP 403 Forbidden**, chặn đứng hoàn toàn việc xem trộm chuyến xe của người khác (`L3-SEC-22`).

#### Câu 82: Kiểm thử tính Idempotency (Chống lặp thao tác) ở API bắt đầu chuyến đi `POST /api/v1/trips/{id}/start`?
* **Hội đồng hỏi**: *"Nếu mạng lag, app tài xế gửi 2 request 'Start Trip' trong 100ms thì API có bị lỗi không?"*
* **Trả lời phản biện**:
  * Request 1 chuyển trạng thái sang `IN_PROGRESS`.
  * Request 2 đến khi trạng thái đã là `IN_PROGRESS` $\implies$ Hệ thống nhận diện trạng thái hiện tại và trả về kết quả thành công với trạng thái hiện hành hoặc ném `409 Conflict`, không bao giờ ghi đè giờ bắt đầu lần 2 (`L3-TRP-18`).

#### Câu 83: Kiểm thử ngăn chặn tấn công SQL Injection qua tham số Search?
* **Trả lời phản biện**:
  * Gửi `GET /api/v1/stores?keyword=' OR '1'='1` $\implies$ Tham số được truyền qua JPA Parameter Binding chuẩn $\implies$ Hệ thống tìm cửa hàng có tên chứa chuỗi ký tự đó và trả về mảng rỗng, không làm lộ dữ liệu.

#### Câu 84: Kiểm thử ngăn chặn việc Admin tự tước quyền hoặc tự khóa tài khoản của chính mình?
* **Trả lời phản biện**:
  * Admin đang đăng nhập gửi `PATCH /api/v1/users/{my-id}/status` với `isActive = false` $\implies$ Controller phát hiện trùng ID người thao tác $\implies$ Trả về **HTTP 400 Bad Request** với thông điệp `CANNOT_DEACTIVATE_SELF` (`L3-USE-12`).

#### Câu 85: Kiểm thử phân quyền chi tiết (Granular Permission Matrix - V28)?
* **Trả lời phản biện**:
  * Một User có role `DISPATCHER` nhưng bị thu hồi quyền `TRIP_DISPATCH` trong bảng `role_permissions` $\implies$ Khi gọi `POST /api/v1/trips/1/dispatch` bị từ chối HTTP 403 ngay lập tức (`L3-SEC-15`).

---

# PHẦN 4: L4 — END-TO-END (E2E) TESTING (30 CÂU HỎI & BẰNG CHỨNG)

### 🟢 Mức độ Dễ (Câu 91 – 100: Cypress 15, Setup & Thao tác UI)

#### Câu 91: E2E Test (L4) trong dự án ELog là gì và dùng công cụ nào?
* **Trả lời phản biện**:
  * L4 kiểm thử toàn diện trải nghiệm người dùng từ giao diện Web (Frontend Vite/React) tới Backend/Mock API. Dự án sử dụng **Cypress 15.21 (Headless Electron / Chrome)** cho Web và **Flutter Integration Test** cho Mobile App.

#### Câu 92: Dự án có bao nhiêu kịch bản E2E Test và kết quả chạy thực tế ra sao?
* **Trả lời phản biện**:
  * Dự án có **42 Web E2E Test Scenarios** (chia thành 15 test suites từ `sc01` đến `sc14` và `uat-master-e2e-suite.cy.ts`) + **8 Mobile Scenarios**.
  * Kết quả chạy thực tế trên Cypress 15: **42/42 Pass (100%)** với thời gian chạy là **1 phút 28 giây**.

#### Câu 93: Làm sao để chạy toàn bộ bộ test Cypress từ dòng lệnh?
* **Trả lời phản biện**:
  * Chạy lệnh: `npx cypress run --spec "cypress/e2e/uat-scenarios/*.cy.ts"`

#### Câu 94: File cấu hình chính của Cypress nằm ở đâu?
* **Trả lời phản biện**:
  * File [cypress.config.js](file:///d:/FULearning/semester%209/Elog/cypress.config.js) cấu hình `baseUrl: 'http://localhost:5173'`, `supportFile: 'cypress/support/e2e.js'`, viewport `1280x720`.

#### Câu 95: Lệnh `cy.visit('/dispatcher/import')` làm nhiệm vụ gì?
* **Trả lời phản biện**:
  * Điều hướng trình duyệt ảo đến trang Nhập đơn hàng của Điều phối viên.

#### Câu 96: Làm sao để giả lập thao tác đính kèm file Excel trong Cypress?
* **Trả lời phản biện**:
  * Dùng `cy.get('input[type="file"]').selectFile('Report5/2026-08-16-Report5/mau_import_150_don_hang_3_ngay.xlsx', { force: true })`.

#### Câu 97: Làm sao để kiểm tra một thông báo Toast thành công hiển thị trên màn hình Ant Design?
* **Trả lời phản biện**:
  * `cy.get('.ant-message-success', { timeout: 8000 }).should('be.visible').and('contain', 'Import hoàn tất')`.

#### Câu 98: `cy.wait('@alias')` có tác dụng gì?
* **Trả lời phản biện**:
  * Tạm dừng kịch bản test để chờ đợi một network request (đã được định danh bằng `.as('alias')`) hoàn thành, giúp loại bỏ lỗi bất đồng bộ.

#### Câu 99: Cypress lưu video và ảnh chụp màn hình khi test fail ở đâu?
* **Trả lời phản biện**:
  * Lưu tại thư mục `cypress/screenshots/` và `cypress/videos/`.

#### Câu 100: Custom Command `cy.loginAs('dispatcher01')` hoạt động thế nào?
* **Trả lời phản biện**:
  * Được định nghĩa trong `cypress/support/commands.js`, tự động chèn JWT token vào `localStorage` hoặc điền form đăng nhập để khởi tạo phiên làm việc nhanh chóng.

---

### 🟡 Mức độ Trung bình (Câu 101 – 110: Mock Intercepts vs Live Backend & UI Assertions)

#### Câu 101: Chiến lược Mock-driven E2E (`cy.intercept`) mang lại lợi ích gì so với chạy thuần Live DB?
* **Trả lời phản biện**:
  * **Tốc độ & Tính ổn định**: Mock-driven chạy toàn bộ 42 kịch bản chỉ mất **1 phút 28 giây**, không bị phụ thuộc vào độ trễ mạng hay rác dữ liệu DB.
  * **Độ bao phủ biên**: Có thể dễ dàng mô phỏng các tình huống lỗi hiếm gặp (mất mạng 500, lỗi xe hỏng, lỗi quá tải) mà Live DB rất khó tái lập liên tục.

#### Câu 102: Kiểm thử kịch bản SC-01 (Nhập đơn hàng 150 dòng) diễn ra như thế nào?
* **Trả lời phản biện**:
  * Cypress truy cập `/dispatcher/import`, tải file [mau_import_150_don_hang_3_ngay.xlsx](file:///d:/FULearning/semester%209/Elog/Report5/2026-08-16-Report5/mau_import_150_don_hang_3_ngay.xlsx), bấm nút **"Tải lên"**. Chờ `@postImport` trả về HTTP 201 $\implies$ Assert bảng lịch sử xuất hiện lô hàng mới với 150 đơn (`ELOG-SC01-01`).

#### Câu 103: Kiểm thử kịch bản SC-02 (Điều chỉnh giờ xuất bến trên UI)?
* **Trả lời phản biện**:
  * Mở Drawer chi tiết chuyến gom `/dispatcher/trip-drafts/101`, đổi giờ xuất bến thành `08:30`, bấm **"Tính lại ETA"** $\implies$ Assert các thẻ điểm dừng cập nhật lại giờ dự kiến mới (`ELOG-SC02-02`).

#### Câu 104: Kiểm thử kịch bản SC-03 (Tách chuyến xe quá tải)?
* **Trả lời phản biện**:
  * Xem chuyến xe bị cảnh báo vượt tải trọng (đỏ) $\implies$ Bấm nút **"Tách chuyến"** $\implies$ Modal tách chuyến mở ra, chọn 2 đơn hàng chuyển sang chuyến mới $\implies$ Assert chuyến cũ trở về trạng thái hợp lệ (xanh) và sinh ra chuyến Draft phụ (`ELOG-SC03-02`).

#### Câu 105: Kiểm thử kịch bản SC-04 (Gán xe và tài xế bằng Select Dropdown)?
* **Trả lời phản biện**:
  * Mở Modal phân tài nguyên, click dropdown chọn tài xế `Le Van Driver`, chọn xe `29A-12345`, bấm **"Xác nhận phân công"** $\implies$ Assert modal đóng và thẻ chuyến hiển thị biển số xe (`ELOG-SC04-01`).

#### Câu 106: Kiểm thử kịch bản SC-05 (Xem và in bảng kê bốc xếp LIFO)?
* **Trả lời phản biện**:
  * Truy cập `/warehouse/manifests/100` $\implies$ Assert bảng kê hiển thị danh sách kiện hàng với thứ tự LIFO, nút "In bảng kê" sẵn sàng (`ELOG-SC05-01`).

#### Câu 107: Kiểm thử kịch bản SC-07 (Tài xế giao hàng và tải ảnh e-PoD)?
* **Trả lời phản biện**:
  * Giả lập giao diện Mobile Web/App: Bấm "Bắt đầu chuyến đi" $\implies$ Đến điểm dừng 1 $\implies$ Tải ảnh bằng chứng giao hàng $\implies$ Bấm "Hoàn thành giao" $\implies$ Assert điểm dừng chuyển sang màu xanh lá (`ELOG-SC07-02`).

#### Câu 108: Kiểm thử kịch bản SC-08 (Cảnh báo xe chạy trễ trên bản đồ thời gian thực)?
* **Trả lời phản biện**:
  * Truy cập Dashboard giám sát `/dispatcher/monitoring` $\implies$ Assert biểu tượng xe hiển thị badge màu vàng (Cảnh báo trễ > 15 phút) và hiển thị thời gian trễ dự kiến (`ELOG-SC08-02`).

#### Câu 109: Kiểm thử kịch bản SC-11 (Quyết toán chuyến đi và sửa đổi số liệu)?
* **Trả lời phản biện**:
  * Điều phối viên mở bảng quyết toán chuyến `/dispatcher/trip-outcomes/100`, bấm "Duyệt quyết toán" $\implies$ Bấm "Sửa đổi số liệu", điền lý do "Khách trả thêm tiền mặt" $\implies$ Assert bản ghi cập nhật thành công (`ELOG-SC11-02`).

#### Câu 110: Kiểm thử kịch bản SC-12 (Lọc biểu đồ KPI Analytics)?
* **Trả lời phản biện**:
  * Truy cập `/manager/kpi`, chọn khoảng ngày tháng 8/2026, chọn tài xế `driver01` $\implies$ Assert các widget KPI (OTD, Tỷ lệ thành công, Tổng km) cập nhật số liệu tương ứng (`ELOG-SC12-02`).

---

### 🔴 Mức độ Khó / Phản biện cao (Câu 111 – 120: Flaky Tests, Race Condition UI & Master Suite)

#### Câu 111: "Flaky Test" trong Cypress là gì và nhóm đã khắc phục triệt để bằng cách nào?
* **Hội đồng hỏi**: *"Có bao giờ test của em lúc chạy Pass, lúc chạy Fail không? Nguyên nhân do đâu và em fix thế nào?"*
* **Trả lời phản biện**:
  * Flaky Test là hiện tượng test không ổn định do phụ thuộc vào timing, animation render của UI, hoặc độ trễ mạng.
  * **Giải pháp khắc phục của nhóm**:
    1. Tuyệt đối không dùng `cy.wait(3000)` cứng. Thay vào đó dùng `cy.wait('@apiAlias')` để chờ đúng thời điểm API phản hồi.
    2. Sử dụng retry-assertions của Cypress: `cy.get('.ant-table-row', { timeout: 8000 }).should('have.length.gt', 0)`.
    3. Đóng băng đồng hồ giả lập thời gian (`cy.clock()`) khi test các kịch bản đếm ngược hoặc timeout.

#### Câu 112: Kịch bản `uat-master-e2e-suite.cy.ts` kiểm thử chuỗi 5 giai đoạn liên hoàn (Continuous Lifecycle) ra sao?
* **Trả lời phản biện**:
  * Kịch bản master chạy xuyên suốt một vòng đời logistics hoàn chỉnh trong **13 giây**:
    * **Stage 1**: Kiểm tra dữ liệu nền và quyền truy cập User.
    * **Stage 2**: Import đơn hàng Excel và duyệt kế hoạch gom chuyến.
    * **Stage 3**: Xuất bảng kê LIFO và bấm lệnh xuất bến.
    * **Stage 4**: Giả lập tài xế thực thi giao hàng từng điểm trên mobile.
    * **Stage 5**: Quyết toán chuyến đi và đối soát số liệu trên báo cáo KPI.

#### Câu 113: Phản biện: "Nếu chỉ chạy Mock `cy.intercept`, làm sao biết giao diện có tương thích với Backend thật không?"
* **Trả lời phản biện**:
  * Để bảo đảm tính tương thích hai chiều, nhóm đã thực hiện **Phương án B**:
    1. Khởi động Backend Spring Boot 8080 và MySQL 3307 thật.
    2. Đã test gọi API trực tiếp từ Frontend/Node client: Đăng nhập JWT, tải 20 Stores, 20 Vehicles, 12 Routes, và Upload thành công file Excel thật 150 đơn (`POST /api/v1/imports` trả về HTTP 201).
    3. Dữ liệu mock trong `cy.intercept` được trích xuất 100% từ cấu trúc DTO thật của Backend, đảm bảo không có sai lệch schema (Zero Contract Drift).

#### Câu 114: Cypress xử lý các phần tử bị che khuất hoặc modal backdrop thế nào?
* **Trả lời phản biện**:
  * Sử dụng `{ force: true }` khi tương tác với `input[type="file"]` bị ẩn bởi component Upload của Ant Design, hoặc assert `.ant-modal-wrap` biến mất hoàn toàn (`should('not.exist')`) trước khi thao tác lên bảng phía sau.

#### Câu 115: Kiểm thử Responsive trên các độ phân giải màn hình khác nhau?
* **Trả lời phản biện**:
  * Sử dụng `cy.viewport('iphone-x')` (375x812) để kiểm thử giao diện Tài xế trên Mobile Web và `cy.viewport(1920, 1080)` để kiểm thử giao diện Điều phối viên trên Desktop.

---

# PHẦN 5: L5 / UAT — USER ACCEPTANCE TESTING (30 CÂU HỎI & BẰNG CHỨNG)

### 🟢 Mức độ Dễ (Câu 121 – 130: Định nghĩa, Kịch bản & Nghiệm thu)

#### Câu 121: UAT (User Acceptance Testing) là gì và ai là người nghiệm thu?
* **Trả lời phản biện**:
  * UAT là tầng kiểm thử chấp nhận của người dùng cuối nhằm xác nhận phần mềm đáp ứng đúng và đầy đủ các yêu cầu nghiệp vụ thực tế (Business Requirements). Người nghiệm thu là Đại diện Chủ hàng, Trưởng phòng Điều phối (Logistics Manager), Thủ kho và Tài xế.

#### Câu 122: Dự án ELog có bao nhiêu kịch bản UAT lớn?
* **Trả lời phản biện**:
  * Có **4 Kịch bản Nghiệp vụ Master (SC-01 đến SC-04)** với **25 - 37 ca kiểm thử nghiệm thu chi tiết** được mô tả trong [Report 5.5_ELog_UAT-Scripts.xlsx](file:///d:/FULearning/semester%209/Elog/Report5/2026-08-16-Report5/Report%205.5_ELog_UAT-Scripts.xlsx).

#### Câu 123: Kịch bản UAT SC-01 đại diện cho quy trình nghiệp vụ nào?
* **Trả lời phản biện**:
  * **SC-01: Order Intake & Planning (Tiếp nhận đơn hàng và Gom chuyến tự động)**: Từ lúc tải file Excel 150 đơn, lọc đơn hợp lệ, gom thành các chuyến dự thảo theo tuyến cố định và kiểm tra sơ bộ tải trọng.

#### Câu 124: Kịch bản UAT SC-02 đại diện cho quy trình nào?
* **Trả lời phản biện**:
  * **SC-02: Capacity Optimization & Resource Assignment (Tối ưu hóa tải trọng và Phân bổ nguồn lực)**: Xử lý quá tải, tách chuyến, gán xe và tài xế phù hợp hạng bằng, sinh bảng kê xếp hàng LIFO.

#### Câu 125: Kịch bản UAT SC-03 đại diện cho quy trình nào?
* **Trả lời phản biện**:
  * **SC-03: Real-time Dispatch & Mobile Execution (Điều phối và Thực thi trên ứng dụng di động)**: Phát lệnh xuất bến, tài xế nhận chuyến, đi theo lộ trình, check-in điểm dừng và nộp ảnh chữ ký e-PoD.

#### Câu 126: Kịch bản UAT SC-04 đại diện cho quy trình nào?
* **Trả lời phản biện**:
  * **SC-04: Exception Handling & Outcome Settlement (Xử lý sự cố và Quyết toán chuyến đi)**: Ghi nhận đơn hàng bị từ chối, giải quyết ticket ngoại lệ, đối soát tiền thu hộ và cập nhật báo cáo hiệu suất KPI.

#### Câu 127: Tiêu chí chấp nhận (Acceptance Criteria) của UAT là gì?
* **Trả lời phản biện**:
  * 100% các ca kiểm thử UAT mức độ Critical/High phải Pass; không còn lỗi chặn luồng (Blocker/Critical Defect); thời gian phản hồi thao tác dưới 2 giây; có đầy đủ bằng chứng thực thi.

#### Câu 128: Biên bản nghiệm thu UAT được lưu trữ ở đâu trong dự án?
* **Trả lời phản biện**:
  * Lưu tại `ELog-BE/test-execution/evidence/uat-approval-review.md` và file Excel [Report 5.5_ELog_UAT-Scripts.xlsx](file:///d:/FULearning/semester%209/Elog/Report5/2026-08-16-Report5/Report%205.5_ELog_UAT-Scripts.xlsx).

#### Câu 129: Khi một bước UAT bị Fail thì quy trình xử lý như thế nào?
* **Trả lời phản biện**:
  * Tạo Defect Ticket (ghi rõ Bước thực hiện, Kết quả thực tế, Kết quả mong muốn, Ảnh bằng chứng) ➔ Dev fix ➔ Chạy lại Regression Test ở tầng L1-L4 ➔ Thực hiện lại bước UAT đó để nghiệm thu lại.

#### Câu 130: UAT trong đồ án này được thực hiện thủ công hay tự động?
* **Trả lời phản biện**:
  * Kết hợp cả hai: Được tự động hóa xác thực thông qua bộ kịch bản **Cypress Master Suite (L4)** và được nghiệm thu nghiệp vụ qua các biên bản kịch bản chi tiết của **Report 5.5**.

---

### 🟡 Mức độ Trung bình (Câu 131 – 140: Xử lý sự cố thực tế & Traceability Matrix)

#### Câu 131: Kịch bản khách hàng hẹn lùi giờ giao (Customer Time Delay) được xử lý thế nào trong UAT?
* **Trả lời phản biện**:
  * Khi khách tại Stop 2 yêu cầu giao sau 14:00 (trong khi chuyến sáng giao lúc 09:00):
    * Điều phối viên dùng tính năng "Settle Delay" ➔ Chọn "Tách đơn sang chuyến chiều" hoặc "Loại trừ đơn khỏi chuyến sáng" (`L4-WEB-PLAN-09`). Chuyến xe được tính lại ETA và tiếp tục xuất phát đúng giờ cho các khách hàng còn lại.

#### Câu 132: Kịch bản xe tải bị hỏng giữa đường (Vehicle Breakdown) được nghiệm thu ra sao?
* **Trả lời phản biện**:
  * Tài xế bấm "Báo sự cố xe" trên App ➔ Điều phối viên nhận cảnh báo khẩn cấp trên Dashboard ➔ Bấm "Hủy chuyến khẩn cấp" / "Điều xe cứu hộ thay thế" ➔ Các đơn hàng chưa giao tự động quay lại trạng thái `PENDING_REDISPATCH` (`SC09-03`).

#### Câu 133: Khách hàng chỉ nhận 3 trên 5 kiện hàng (Partial Delivery Rejection) được ghi nhận thế nào?
* **Trả lời phản biện**:
  * Tài xế nhập số lượng thực nhận = 3, số lượng từ chối = 2, chọn lý do "Hàng bị móp méo", chụp ảnh kiện hỏng ➔ Bấm xác nhận ➔ Hệ thống tạo ticket ngoại lệ và trừ tiền thu hộ tương ứng (`SC09-01`).

#### Câu 134: Ma trận truy vết yêu cầu (Traceability Matrix) là gì?
* **Trả lời phản biện**:
  * Là bảng liên kết trực tiếp giữa **Yêu cầu phần mềm (SRS / User Story)** ➔ **Mã Test Case L1 (Unit)** ➔ **Mã L2/L3 (Integration/API)** ➔ **Mã L4 (E2E)** ➔ **Kịch bản UAT**, đảm bảo không có bất kỳ yêu cầu nào bị bỏ sót kiểm thử.

#### Câu 135: Nghiệm thu tính chính xác của thuật toán xếp dỡ kho LIFO mang lại giá trị gì cho doanh nghiệp?
* **Trả lời phản biện**:
  * Giúp tài xế không phải bốc dỡ toàn bộ thùng xe để tìm kiện hàng của điểm đầu tiên, tiết kiệm **15 - 20 phút** tại mỗi điểm giao, giảm thiểu nguy cơ rơi vỡ hàng hóa.

#### Câu 136: Nghiệm thu việc bảo vệ dữ liệu cá nhân khách hàng (Số điện thoại, địa chỉ)?
* **Trả lời phản biện**:
  * Tài xế chỉ xem được số điện thoại của khách tại điểm dừng hiện tại; khi đã hoàn thành điểm dừng đó, số điện thoại bị ẩn/masking (`0912***678`) để bảo mật thông tin khách hàng.

#### Câu 137: Nghiệm thu quy trình bàn giao ca và xuất bến giữa Thủ kho và Tài xế?
* **Trả lời phản biện**:
  * Thủ kho quét mã QR chuyến xe trên bảng kê LIFO $\implies$ Tài xế đối soát số lượng và ký xác nhận trên mobile $\implies$ Chuyến xe chuyển trạng thái sang `DISPATCHED`.

#### Câu 138: Nghiệm thu tính năng điều chỉnh giờ xuất bến linh hoạt theo tình hình kẹt xe thực tế?
* **Trả lời phản biện**:
  * Cho phép điều phối viên dời giờ xuất bến linh hoạt từ 15 đến 120 phút, hệ thống tự động gọi Goong Maps API tính lại ma trận thời gian di chuyển trong giờ cao điểm.

#### Câu 139: Nghiệm thu độ trễ đồng bộ dữ liệu giữa Mobile App tài xế và Web Dashboard điều phối?
* **Trả lời phản biện**:
  * Khi tài xế bấm "Hoàn thành giao hàng", trạng thái trên Dashboard điều phối cập nhật tức thì trong vòng **dưới 1 giây** qua cơ chế WebSocket / REST Polling.

#### Câu 140: Nghiệm thu các chỉ số báo cáo KPI quản trị cấp cao (Logistics Manager Dashboard)?
* **Trả lời phản biện**:
  * Đối soát các biểu đồ: Tỷ lệ hoàn thành đúng giờ (OTD $\ge 90\%$), Tỷ lệ lấp đầy thùng xe (Capacity Utilization $\ge 80\%$), và Chi phí vận hành trên mỗi km.

---

### 🔴 Mức độ Khó / Phản biện cao (Câu 141 – 150: Đánh giá ROI, Đạo đức kiểm thử & Bài học kinh nghiệm)

#### Câu 141: "Tại sao nhóm đầu tư tới 5 tầng kiểm thử (L1-L5)? Có bị Over-testing không?"
* **Hội đồng hỏi**: *"Làm đồ án tốt nghiệp mà viết tới 643 test case cho cả 5 tầng kiểm thử có tốn quá nhiều thời gian không? ROI (Return on Investment) của việc này là gì?"*
* **Trả lời phản biện**:
  * Hệ thống Logistics ELog có tính chất vận hành liên tục với các ràng buộc tài chính và tải trọng khắt khe (chở quá tải bị phạt vi phạm giao thông, giao trễ bị phạt SLA, sai sót COD gây thất thoát tiền bạc).
  * **ROI đạt được**:
    1. **Bắt lỗi sớm (Shift-Left Testing)**: 363 Unit test giúp bắt 80% lỗi logic ngay khi code, chi phí sửa lỗi rẻ gấp 10 lần so với khi phát hiện ở tầng E2E.
    2. **Tự tin tái cấu trúc (Safe Refactoring)**: Khi nâng cấp thuật toán gom đơn từ Haversine sang Goong API, toàn bộ 363 test case bảo vệ code không bị vỡ logic cũ.
    3. **Tự động hóa hoàn toàn**: Tiết kiệm hàng trăm giờ kiểm thử thủ công mỗi khi release phiên bản mới.

#### Câu 142: Trong quá trình kiểm thử, nhóm đã phát hiện và sửa những con Bug nghiêm trọng nhất nào?
* **Trả lời phản biện**:
  * **Bug 1 (Làm tròn số tải trọng)**: Khi tính tổng thể tích, dùng kiểu `float` bị mất độ chính xác làm xe bị quá tải 0.01m3 mà hệ thống vẫn cho qua $\implies$ Đã chuyển toàn bộ sang `BigDecimal` và bắt chặt bằng test `L1-CAP-04`.
  * **Bug 2 (Xóa tầng Cascade)**: Xóa Stop làm văng lỗi ràng buộc khóa ngoại với bảng `delivery_order_results` $\implies$ Đã fix bằng migration `V50` và test `L2-OUT-04`.
  * **Bug 3 (Flaky UI Modal)**: Nút chọn xe trên modal Ant Design bị click trượt do animation $\implies$ Đã fix bằng `cy.intercept` và wait alias trong test `sc04`.

#### Câu 143: "Nếu ngày mai hệ thống có thêm tính năng giao hàng lạnh (Cold-Chain) với yêu cầu kiểm soát nhiệt độ xe, bộ test sẽ thay đổi thế nào?"
* **Trả lời phản biện**:
  * Dễ dàng mở rộng nhờ kiến trúc kiểm thử phân tầng:
    * **L1**: Thêm test case vào `ConstraintValidationServiceImplTest` kiểm tra ràng buộc nhiệt độ sản phẩm ($T_{min}, T_{max}$) với loại thùng xe lạnh (`L1-CON-10..15`).
    * **L2**: Thêm cột `temperature_min`, `temperature_max` vào migration Flyway và test truy vấn xe lạnh.
    * **L3**: Thêm validate payload nhiệt độ ở API.
    * **L4/UAT**: Thêm kịch bản tài xế chụp ảnh đồng hồ nhiệt kế khi giao hàng.

#### Câu 144: Độ tin cậy của bộ kiểm thử: Làm sao chứng minh nhóm không viết test "cho có" (Test chỉ `assertTrue(true)`)?
* **Trả lời phản biện**:
  * Dự án áp dụng quy chuẩn kiểm toán khắt khe:
    1. Mọi test case đều có assertion rõ ràng trên dữ liệu nghiệp vụ thật (`assertEquals`, `assertThrows`).
    2. Toàn bộ các class test giả (Fake tests `assertTrue(true)`) thời kỳ đầu đã bị loại bỏ hoàn toàn trong đợt tái cấu trúc kiểm toán.
    3. Công cụ JaCoCo đo lường cả **Branch Coverage** (độ bao phủ nhánh if/else) đạt trên 85%, chứng minh mọi rẽ nhánh logic đều được thực thi thật sự.

#### Câu 145: Làm sao để đảm bảo an toàn bảo mật dữ liệu khi chạy test trên môi trường CI/CD?
* **Trả lời phản biện**:
  * Không hardcode mật khẩu hay secret key trong file test. Toàn bộ `JWT_SECRET`, database password được nạp qua biến môi trường (Environment Variables) hoặc sử dụng secret mặc định riêng biệt cho môi trường dev/test.

#### Câu 146: Nếu hệ thống nâng cấp từ MySQL 9.7 lên PostgreSQL trong tương lai thì tầng test nào bị ảnh hưởng nhiều nhất?
* **Trả lời phản biện**:
  * Tầng **L1 (Unit Test)** và **L4 (E2E Test)** hoàn toàn không bị ảnh hưởng vì L1 dùng mock và L4 tương tác qua Web UI/HTTP.
  * Tầng **L2 (Integration Test)** sẽ là tầng duy nhất được chạy lại để kiểm tra cú pháp dialect SQL của Hibernate và các migration của Flyway.

#### Câu 147: Đạo đức kiểm thử: Khi thời hạn nộp đồ án cận kề mà còn 3 test case bị fail, em sẽ làm gì?
* **Trả lời phản biện**:
  * Tuyệt đối không xóa test case hoặc sửa assertion thành sai để "ép pass ảo".
  * Phải đánh dấu đúng trạng thái `Fail`, ghi rõ nguyên nhân vào sổ kiểm toán (Defect Ledger), báo cáo trung thực với giảng viên hướng dẫn và tập trung phân tích nguyên nhân gốc rễ (Root Cause Analysis) để sửa code cho đến khi Pass thật sự.

#### Câu 148: Tỷ lệ phân bổ số lượng test case giữa các tầng có hợp lý theo mô hình Kim tự tháp kiểm thử (Test Pyramid) không?
* **Trả lời phản biện**:
  * Hoàn toàn chuẩn theo mô hình Kim tự tháp kiểm thử (Mike Cohn's Test Pyramid):
    * **Đáy tháp (L1 Unit Test)**: Chiếm số lượng lớn nhất (**363 cases - 56.4%**), chạy nhanh nhất (2.8s).
    * **Thân tháp (L2 & L3 Integration/API)**: Chiếm số lượng vừa (**205 cases - 31.9%**).
    * **Đỉnh tháp (L4 E2E & UAT)**: Tập trung vào các hành trình người dùng trọng yếu (**75 cases - 11.7%**).

#### Câu 149: Đánh giá cá nhân: Điều gì làm em tự hào nhất về bộ kiểm thử của đồ án này?
* **Trả lời phản biện**:
  * Đó là tính **toàn vẹn và tự động hóa tuyệt đối (100% Traceability & Automation)**. Toàn bộ 643 test case từ Unit test, Integration test, API test đến E2E test đều có thể chạy tự động bằng 1 dòng lệnh, có cơ chế script kiểm toán độc lập `validate-results.mjs` bảo vệ tính toàn vẹn của dữ liệu và đạt tỷ lệ **100% Pass** trên môi trường thực tế.

#### Câu 150: Lời kết bảo vệ: Hãy tóm tắt chất lượng phần mềm ELog trong 3 câu ngắn gọn!
* **Trả lời phản biện**:
  1. *"ELog là hệ thống quản lý giao hàng có logic nghiệp vụ chặt chẽ, được bảo vệ bởi **643 ca kiểm thử tự động 5 tầng** từ mã nguồn Java đến giao diện người dùng."*
  2. *"Hệ thống đảm bảo tính toàn vẹn dữ liệu, tối ưu hóa tải trọng xe LIFO, phân quyền bảo mật RBAC và xử lý mượt mà toàn bộ chu trình 150 đơn hàng thực tế."*
  3. *"Mọi số liệu kiểm thử trong báo cáo đều có bằng chứng thực thi xác thực (Surefire XML, JaCoCo, Cypress logs) sẵn sàng cho việc nghiệm thu và triển khai thực tế."*

---

# PHẦN 6: MASTER SOURCE CODE INDEX — DANH MỤC TOÀN BỘ FILE MÃ NGUỒN KIỂM THỬ

Bảng danh mục dưới đây tổng hợp đầy đủ toàn bộ các file mã nguồn kiểm thử trong toàn bộ dự án ELog, giúp người đánh giá và sinh viên có thể tra cứu và click trực tiếp vào file mã nguồn:

### 1. Tầng L1: Backend Service Unit Tests (`ELog-BE/src/test/java/com/elog/service/`)

| STT | Domain Sheet | Class File Kiểm Thử | Số Case | Mục Đích Kiểm Thử |
| :---: | :--- | :--- | :---: | :--- |
| 1 | `AuthService` | [AuthServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/AuthServiceImplTest.java) | 12 | Đăng nhập JWT, refresh token, đổi mật khẩu, mã hóa BCrypt |
| 2 | `ImportService` | [ImportServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/ImportServiceImplTest.java) | 17 | Đọc Excel 150 đơn, validate định dạng, xuất báo cáo lỗi |
| 3 | `TripDraftService` | [TripDraftServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/TripDraftServiceImplTest.java) | 34 | Gom đơn tự động, khóa chuyến, loại trừ đơn trễ, tách chuyến |
| 4 | `TripService` | [TripServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/TripServiceImplTest.java) | 33 | Gán xe/tài xế, kiểm tra bằng lái, phát lệnh xuất bến, hủy chuyến |
| 5 | `CapacityValidationService` | [CapacityValidationServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/CapacityValidationServiceImplTest.java) | 27 | Kiểm tra tải trọng, thể tích, hệ số đệm an toàn 95% |
| 6 | `ConstraintValidationService` | [ConstraintValidationServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/ConstraintValidationServiceImplTest.java) | 9 | Ràng buộc khung giờ cửa hàng, giới hạn tải trọng đường |
| 7 | `RecommendationService` | [RecommendationServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/RecommendationServiceImplTest.java) | 21 | Thuật toán gợi ý xe đơn và cơ chế Fallback ghép 2 xe |
| 8 | `DriverTripService` | [DriverTripServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/DriverTripServiceImplTest.java) | 35 | Bắt đầu chuyến, check-in điểm dừng, nộp ảnh PoD, từ chối giao |
| 9 | `TripMonitoringService` | [TripMonitoringServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/TripMonitoringServiceImplTest.java) | 21 | Giám sát GPS thời gian thực, phát hiện cảnh báo trễ |
| 10 | `ExceptionService` | [ExceptionServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/ExceptionServiceImplTest.java) | 19 | Tạo và xử lý ticket ngoại lệ giao hàng, xe hỏng |
| 11 | `RouteService` | [RouteServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/RouteServiceImplTest.java) | 19 | Quản lý tuyến đường cố định, thứ tự điểm dừng |
| 12 | `DispatchExportService` | [DispatchExportServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/DispatchExportServiceImplTest.java) | 8 | Xuất danh sách lệnh điều phối ra file Excel |
| 13 | `ManifestService` | [ManifestServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/ManifestServiceImplTest.java) | 10 | Thuật toán sinh bảng kê xếp dỡ hàng ngược chiều LIFO |
| 14 | `KpiService` | [KpiServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/KpiServiceImplTest.java) | 9 | Tính toán chỉ số OTD, tỷ lệ thành công, quãng đường |
| 15 | `VehicleService` | [VehicleServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/VehicleServiceImplTest.java) | 13 | Quản lý đội xe, tải trọng, kích thước thùng xe, bảo dưỡng |
| 16 | `DriverStatusService` | [DriverStatusServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/DriverStatusServiceImplTest.java) | 10 | Cập nhật trạng thái sẵn sàng, nghỉ phép của tài xế |
| 17 | `StoreService` | [StoreServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/StoreServiceImplTest.java) | 9 | Quản lý danh mục cửa hàng, tọa độ GPS, khung giờ nhận |
| 18 | `ProductService` | [ProductServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/ProductServiceImplTest.java) | 11 | Danh mục sản phẩm, trọng lượng, thể tích quy đổi SKU |
| 19 | `UserService` | [UserServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/UserServiceImplTest.java) | 15 | Quản lý tài khoản người dùng, kích hoạt/khóa tài khoản |
| 20 | `RoleService` | [RoleServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/RoleServiceImplTest.java) | 8 | Quản lý vai trò và ma trận phân quyền hệ thống |
| 21 | `TripStateMachine` | [TripStateMachineTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/TripStateMachineTest.java) | 7 | Kiểm thử ma trận chuyển đổi trạng thái chuyến đi |
| 22 | `TripOutcomeServices`| [TripOutcomeServiceImplTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/TripOutcomeServiceImplTest.java) | 9 | Quyết toán chuyến, thu hộ COD, sửa đổi số liệu kiểm toán |
| 23 | `MapAndEtaServices` | [HaversineEtaCalculatorTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/impl/HaversineEtaCalculatorTest.java) & [GoongMapServiceTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/service/GoongMapServiceTest.java) | 7 | Tính toán khoảng cách địa lý, thời gian di chuyển và ETA |
| **Tổng L1** | **23 Sheets** | **32 Test Classes** | **363** | **100% Pass** |

---

### 2. Tầng L2: Backend Integration Tests (`ELog-BE/src/test/java/com/elog/integration/`)

| STT | Class File Kiểm Thử | Số Case | Mục Đích Kiểm Thử |
| :---: | :--- | :---: | :--- |
| 1 | [TripDraftServiceImplIntegrationTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/integration/TripDraftServiceImplIntegrationTest.java) | 8 | Tương tác gom đơn và lưu bảng `trip_drafts` trong MySQL |
| 2 | [TripIntegrationTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/integration/TripIntegrationTest.java) | 10 | Lưu và cập nhật trạng thái chuyến xe, quan hệ Stop |
| 3 | [OrderImportIntegrationTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/integration/OrderImportIntegrationTest.java) | 8 | Nhập đơn vào bảng `orders`, `import_batches` |
| 4 | [ManifestIntegrationTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/integration/ManifestIntegrationTest.java) | 6 | Sinh bảng kê LIFO và lưu bảng `manifests` |
| 5 | [VehicleDriverIntegrationTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/integration/VehicleDriverIntegrationTest.java) | 8 | Ràng buộc khóa ngoại xe và tài xế trong DB |
| 6 | [StoreRouteIntegrationTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/integration/StoreRouteIntegrationTest.java) | 7 | Tọa độ cửa hàng, thứ tự điểm dừng trên tuyến |
| 7 | [ExceptionIntegrationTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/integration/ExceptionIntegrationTest.java) | 6 | Tạo và cập nhật bảng `delivery_exceptions` |
| 8 | [OutcomeSettlementIntegrationTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/integration/OutcomeSettlementIntegrationTest.java) | 6 | Quyết toán và lưu bảng `trip_outcomes` |
| 9 | [UserSecurityIntegrationTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/integration/UserSecurityIntegrationTest.java) | 8 | Bảng `users`, `roles`, `permissions`, `refresh_tokens` |
| 10 | Các integration test repository khác | 8 | Các query JPA Custom và Specification |
| **Tổng L2** | **Tầng Integration DB** | **28 Test Classes** | **75** | **100% Pass** |

---

### 3. Tầng L3: Backend REST Controller API Tests (`ELog-BE/src/test/java/com/elog/controller/`)

| STT | Class File Kiểm Thử Controller | Số Case | Endpoint Kiểm Thử |
| :---: | :--- | :---: | :--- |
| 1 | [AuthControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/AuthControllerTest.java) | 10 | `/api/v1/auth/login`, `/refresh`, `/logout` |
| 2 | [ImportControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/ImportControllerTest.java) | 8 | `/api/v1/imports`, `/history/{id}` |
| 3 | [TripDraftControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/TripDraftControllerTest.java) | 14 | `/api/v1/trip-drafts`, `/consolidate`, `/confirm` |
| 4 | [TripControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/TripControllerTest.java) | 12 | `/api/v1/trips`, `/assign`, `/dispatch`, `/cancel` |
| 5 | [DriverTripControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/DriverTripControllerTest.java) | 15 | `/api/v1/driver-trips/*`, `/trip-stops/*/arrive` |
| 6 | [ManifestControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/ManifestControllerTest.java) | 6 | `/api/v1/manifests/*` |
| 7 | [ExceptionControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/ExceptionControllerTest.java) | 8 | `/api/v1/exceptions/*`, `/resolve` |
| 8 | [TripOutcomeControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/TripOutcomeControllerTest.java) | 8 | `/api/v1/trip-outcomes/*`, `/amend` |
| 9 | [KpiControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/KpiControllerTest.java) | 7 | `/api/v1/kpi/dashboard`, `/driver-performance` |
| 10 | [StoreControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/StoreControllerTest.java) | 7 | `/api/v1/stores/*` |
| 11 | [VehicleControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/VehicleControllerTest.java) | 8 | `/api/v1/vehicles/*` |
| 12 | [RouteControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/RouteControllerTest.java) | 8 | `/api/v1/routes/*` |
| 13 | [UserControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/UserControllerTest.java) | 10 | `/api/v1/users/*`, `/status` |
| 14 | [ProductControllerTest.java](file:///d:/FULearning/semester%209/Elog/ELog-BE/src/test/java/com/elog/controller/ProductControllerTest.java) | 9 | `/api/v1/products/*` |
| **Tổng L3** | **REST Controllers** | **23 Controller Classes** | **130** | **100% Pass** |

---

### 4. Tầng L4 & UAT: Frontend Cypress E2E Tests (`cypress/e2e/uat-scenarios/`)

| STT | File Kịch Bản Cypress | Số Case | Hành Trình Nghiệp Vụ Kiểm Thử |
| :---: | :--- | :---: | :--- |
| 1 | [sc01-order-intake.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc01-order-intake.cy.ts) | 3 | Nhập file Excel 150 đơn, bắt lỗi dòng sai, chặn ngày quá khứ |
| 2 | [sc02-trip-draft-review.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc02-trip-draft-review.cy.ts) | 3 | Xem điểm dừng, điều chỉnh giờ xuất bến, loại đơn hàng trễ |
| 3 | [sc03-capacity-and-split.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc03-capacity-and-split.cy.ts) | 3 | Kiểm tra tải trọng, tách chuyến quá tải, hoàn tác về draft |
| 4 | [sc04-resource-assignment.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc04-resource-assignment.cy.ts) | 3 | Phân tài xế & xe, chặn xe bận / tài xế không đủ bằng lái |
| 5 | [sc05-warehouse-manifest.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc05-warehouse-manifest.cy.ts) | 2 | Sinh bảng kê xếp hàng LIFO, xem danh sách kiện hàng |
| 6 | [sc06-dispatch-handoff.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc06-dispatch-handoff.cy.ts) | 2 | Điều phối chuyến xe, khóa không cho sửa đổi khi đã xuất bến |
| 7 | [sc07-driver-execution.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc07-driver-execution.cy.ts) | 3 | Tài xế nhận chuyến, check-in điểm dừng, nộp ảnh chữ ký e-PoD |
| 8 | [sc08-monitoring-alerts.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc08-monitoring-alerts.cy.ts) | 2 | Giám sát vị trí xe trên bản đồ, cảnh báo tự động khi xe chạy trễ |
| 9 | [sc09-exception-handling.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc09-exception-handling.cy.ts) | 3 | Khách từ chối nhận hàng, xử lý ticket ngoại lệ, xe hỏng |
| 10 | [sc10-trip-completion.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc10-trip-completion.cy.ts) | 2 | Hoàn thành điểm cuối, xác nhận về kho, giải phóng tài nguyên xe |
| 11 | [sc11-outcome-validation.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc11-outcome-validation.cy.ts) | 3 | Duyệt quyết toán chuyến đi, sửa đổi số liệu có lý do bắt buộc |
| 12 | [sc12-kpi-analytics.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc12-kpi-analytics.cy.ts) | 2 | Báo cáo hiệu suất KPI, lọc theo tài xế và khoảng thời gian |
| 13 | [sc13-master-data.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc13-master-data.cy.ts) | 3 | Quản lý tuyến đường, cửa hàng, tạo hồ sơ xe tải |
| 14 | [sc14-access-governance.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/sc14-access-governance.cy.ts) | 3 | Kiểm tra phân quyền RBAC, chặn Admin tự khóa mình, Đăng xuất |
| 15 | [uat-master-e2e-suite.cy.ts](file:///d:/FULearning/semester%209/Elog/cypress/e2e/uat-scenarios/uat-master-e2e-suite.cy.ts) | 5 | Kịch bản Master 5 giai đoạn xuyên suốt toàn bộ vòng đời |
| 16 | Mobile Flutter Integration Specs | 8 | Đăng nhập tài xế, xem lộ trình trên bản đồ di động |
| **Tổng L4/UAT** | **Web & Mobile E2E** | **16 Spec Files** | **50** | **100% Pass** |

---

### 🏆 BẢNG TỔNG KẾT TOÀN DIỆN DỰ ÁN

| Tầng kiểm thử | Tên gọi | Phạm vi công nghệ | Số lượng Test Case | Trạng thái |
| :---: | :--- | :--- | :---: | :---: |
| **L1** | **Unit Testing** | JUnit 5 + Mockito + JaCoCo | **363** | ✅ **100% Pass** |
| **L2** | **Integration Testing** | Spring Boot Test + MySQL 9.7 + Flyway | **75** | ✅ **100% Pass** |
| **L3** | **System / API Testing** | MockMvc + REST Controller + JWT RBAC | **130** | ✅ **100% Pass** |
| **L4** | **End-to-End (E2E) Testing**| Cypress 15 + Live Vite 5173 + Flutter | **50** | ✅ **100% Pass** |
| **UAT** | **Acceptance Testing** | Nghiệm thu 4 kịch bản nghiệp vụ Master | **25** | ✅ **100% Pass** |
| **TỔNG CỘNG** | **5 TẦNG KIỂM THỬ** | **Toàn bộ hệ sinh thái ELog** | **643** | ✅ **100% PASS** |
