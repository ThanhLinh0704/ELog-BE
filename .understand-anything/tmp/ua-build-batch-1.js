#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');

const projectRoot = path.resolve(process.argv[2]);
const extractPath = path.resolve(process.argv[3]);
const outputPath = path.resolve(process.argv[4]);
const migrationRoot = 'src/main/resources/db/migration';
const p = (name) => `${migrationRoot}/${name}`;
const migrationId = (filePath) => `table:${filePath}:migration`;
const tableId = (filePath, tableName) => `table:${filePath}:${tableName}`;

const P = {
  v1: p('V1__init_schema.sql'),
  v10: p('V10__seed_us08_import_data.sql'),
  v11: p('V11__import_errors_extend.sql'),
  v12: p('V12__trip_draft_schema.sql'),
  v13: p('V13__seed_us10_trip_draft_data.sql'),
  v14: p('V14__trip_draft_review_fields.sql'),
  v15: p('V15__capacity_validation_fields.sql'),
  v16: p('V16__manifest_schema.sql'),
  v17: p('V17__trip_tripstop_schema.sql'),
  v18: p('V18__progress_schema_update.sql'),
  v19: p('V19__init_vietnamese_administrative_units.sql'),
  v2: p('V2__seed_reference_data.sql'),
  v20: p('V20__alter_stores_address_relational.sql'),
  v21: p('V21__add_actual_departure_time_to_trip_stops.sql'),
  v22: p('V22__add_new_fields_for_additional_business_needs.sql'),
  v23: p('V23__remove_uq_batch_active_date.sql'),
  v24: p('V24__add_order_time_window_and_recipient_fields.sql'),
  v25: p('V25__alter_vehicles_and_users_for_license_matching.sql'),
  v26: p('V26__refresh_seed_data_for_northern_region.sql'),
  v27: p('V27__fix_stores_ward_codes.sql'),
  v28: p('V28__add_permissions_schema.sql'),
  v29: p('V29__add_product_permissions.sql'),
  v3: p('V3__update_user_roles_and_email.sql'),
  v30: p('V30__add_store_constraints_and_waiting_time_fields.sql'),
  v31: p('V31__add_order_time_override_fields.sql'),
};

const migrationMeta = {
  [P.v1]: {
    summary: 'Khởi tạo schema quan hệ cốt lõi cho người dùng, cửa hàng, tuyến giao hàng, phương tiện, sản phẩm, đơn hàng, chuyến đi và bằng chứng giao nhận.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'khởi-tạo-schema', 'flyway'],
    languageNotes: 'MySQL DDL tổ chức các bảng theo quan hệ khóa ngoại và bổ sung index cho những truy vấn theo tuyến, đơn hàng, chuyến đi và ngoại lệ.',
  },
  [P.v10]: {
    summary: 'Nạp bộ dữ liệu US-08 gồm batch import, đơn hàng, chi tiết đơn hợp lệ và các lỗi bị từ chối để mô phỏng quy trình nhập đơn từ Excel.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'dữ-liệu-mẫu', 'import-đơn-hàng'],
  },
  [P.v11]: {
    summary: 'Mở rộng bảng import_errors với mã lỗi và tên trường, đồng thời thêm index phục vụ tra cứu lỗi theo batch và loại lỗi.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'kiểm-tra-import', 'indexing'],
  },
  [P.v12]: {
    summary: 'Tạo schema trip_drafts và trip_draft_stops cho bước hợp nhất đơn theo tuyến, đồng thời liên kết đơn hàng với bản nháp chuyến đi.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'lập-kế-hoạch-chuyến', 'schema'],
  },
  [P.v13]: {
    summary: 'Nạp dữ liệu mẫu US-10 cho hai bản nháp chuyến, các điểm dừng tương ứng và liên kết các đơn đã được chấp nhận vào từng bản nháp.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'dữ-liệu-mẫu', 'trip-draft'],
  },
  [P.v14]: {
    summary: 'Bổ sung cấu hình kho, thời gian phục vụ, ETA và các trường review/override để điều phối viên rà soát và tinh chỉnh bản nháp chuyến.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'review-chuyến', 'eta', 'cấu-hình'],
  },
  [P.v15]: {
    summary: 'Thêm kết quả kiểm tra tải trọng và thể tích cùng dấu vết người, thời điểm xác nhận năng lực chuyên chở cho trip_drafts.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'kiểm-tra-tải', 'validation'],
  },
  [P.v16]: {
    summary: 'Tạo manifest và manifest_lines, thêm index phục vụ thứ tự xếp hàng LIFO và nạp một manifest mẫu từ trip draft đã xác nhận.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'manifest', 'lifo', 'dữ-liệu-mẫu'],
    languageNotes: 'Schema lưu cả thứ tự điểm dừng lẫn lifo_sequence để biểu diễn thứ tự chất hàng ngược với thứ tự giao.',
  },
  [P.v17]: {
    summary: 'Tái tạo trips và trip_stops theo schema vận hành mới, rồi nối lại các khóa ngoại từ manifest, bản ghi giao hàng và ngoại lệ.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'chuyến-giao-hàng', 'tái-tạo-schema'],
    languageNotes: 'Migration dùng mẫu drop-and-recreate có kiểm soát, tháo khóa ngoại trước khi thay schema rồi khôi phục quan hệ và index.',
  },
  [P.v18]: {
    summary: 'Tối ưu truy vấn tiến độ bằng index cho chuyến và điểm dừng, đồng thời tạo delivery_exceptions để theo dõi và xử lý sự cố giao hàng.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'theo-dõi-tiến-độ', 'ngoại-lệ', 'indexing'],
  },
  [P.v19]: {
    summary: 'Khởi tạo mô hình địa giới hành chính Việt Nam theo vùng, cấp hành chính, tỉnh, huyện và xã/phường, sau đó nạp bộ dữ liệu tham chiếu đầy đủ.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'địa-giới-hành-chính', 'dữ-liệu-tham-chiếu', 'flyway'],
    languageNotes: 'Dữ liệu được tổ chức theo hierarchy vùng → tỉnh → huyện → xã/phường với mã hành chính và index trên các khóa cha; phần seed lớn được chia thành nhiều INSERT theo lô.',
  },
  [P.v2]: {
    summary: 'Nạp dữ liệu tham chiếu ban đầu cho vai trò, tài khoản, cửa hàng, tuyến, điểm dừng, phương tiện và sản phẩm để hệ thống có thể chạy thử.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'dữ-liệu-mẫu', 'dữ-liệu-tham-chiếu'],
  },
  [P.v20]: {
    summary: 'Chuẩn hóa địa chỉ cửa hàng thành các mã tỉnh, huyện, xã/phường và chi tiết địa chỉ, rồi ràng buộc chúng với schema hành chính Việt Nam.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'chuẩn-hóa-địa-chỉ', 'foreign-key'],
    languageNotes: 'Migration chuyển dữ liệu địa chỉ dạng text sang mô hình quan hệ trước khi xóa cột address cũ và thêm ba khóa ngoại hành chính.',
  },
  [P.v21]: {
    summary: 'Bổ sung actual_departure_time cho trip_stops để schema cơ sở dữ liệu đồng bộ với JPA entity theo dõi thời điểm rời điểm giao.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'đồng-bộ-jpa', 'tiến-trình-giao-hàng'],
  },
  [P.v22]: {
    summary: 'Mở rộng cửa hàng, phương tiện và sản phẩm với khung giờ nhận hàng, giới hạn tải, hình ảnh, giấy phép và thuộc tính đóng gói phục vụ nghiệp vụ bổ sung.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'thuộc-tính-nghiệp-vụ', 'catalog'],
  },
  [P.v23]: {
    summary: 'Gỡ unique index theo ngày giao trên import_batches để cho phép nhiều batch đang hoạt động cùng ngày trong mô hình import tích lũy.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'import-tích-lũy', 'ràng-buộc'],
  },
  [P.v24]: {
    summary: 'Bổ sung khung giờ giao, thông tin người nhận và ghi chú cho orders để lưu yêu cầu giao hàng chi tiết.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'đơn-hàng', 'khung-giờ-giao'],
  },
  [P.v25]: {
    summary: 'Chuẩn hóa thông số phương tiện, sinh mã xe và thêm hạng bằng lái cho người dùng để đối chiếu điều kiện điều khiển xe.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'phương-tiện', 'bằng-lái', 'data-backfill'],
  },
  [P.v26]: {
    summary: 'Làm mới dữ liệu demo cho khu vực miền Bắc bằng cách xóa dữ liệu giao dịch cũ rồi nạp lại sản phẩm, xe, tài xế, 50 cửa hàng, ba tuyến và các điểm dừng.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'dữ-liệu-mẫu', 'khu-vực-miền-bắc', 'reset-dữ-liệu'],
    languageNotes: 'Migration seed có tính phá hủy chủ động: TRUNCATE theo thứ tự phụ thuộc rồi tái nạp toàn bộ dữ liệu tham chiếu và vận hành nhất quán.',
  },
  [P.v27]: {
    summary: 'Sửa mã huyện và xã/phường cho 13 cửa hàng miền Bắc để các địa chỉ tham chiếu đúng bộ mã hành chính.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'sửa-dữ-liệu', 'địa-giới-hành-chính'],
  },
  [P.v28]: {
    summary: 'Tạo schema permissions và role_permissions, sau đó nạp tập quyền nền tảng theo từng vai trò vận hành của hệ thống.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'phân-quyền', 'rbac', 'dữ-liệu-mẫu'],
  },
  [P.v29]: {
    summary: 'Bổ sung quyền đọc/ghi sản phẩm và gán chúng cho các vai trò phù hợp trong mô hình RBAC hiện có.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'phân-quyền-sản-phẩm', 'rbac'],
  },
  [P.v3]: {
    summary: 'Thêm email duy nhất cho người dùng, chuyển quan hệ user-role sang bảng nối nhiều-nhiều và đổi tên vai trò ADMIN thành SYSTEM_ADMIN.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'người-dùng', 'phân-quyền', 'many-to-many'],
  },
  [P.v30]: {
    summary: 'Bổ sung khung giờ và hạn chế loại xe cho cửa hàng, đồng thời lưu thời gian chờ dự kiến và mã vi phạm trên điểm dừng dự kiến lẫn thực tế.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'ràng-buộc-cửa-hàng', 'thời-gian-chờ'],
  },
  [P.v31]: {
    summary: 'Thêm trạng thái, lý do, người thực hiện và thời điểm override khung giờ giao cho orders để hỗ trợ truy vết quyết định điều phối.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'đơn-hàng', 'override-thời-gian', 'audit'],
  },
};

const tableSummaries = {
  roles: 'Lưu danh mục vai trò dùng để phân nhóm quyền truy cập của người dùng.',
  users: 'Lưu tài khoản, thông tin xác thực và trạng thái hoạt động của người dùng hệ thống.',
  stores: 'Lưu cửa hàng nhận hàng cùng địa chỉ, tọa độ và các ràng buộc phục vụ giao nhận.',
  routes: 'Lưu các tuyến giao hàng cố định được điều phối từ kho đến chuỗi cửa hàng.',
  route_stops: 'Biểu diễn thứ tự cửa hàng trên từng tuyến giao hàng và thời gian phục vụ dự kiến.',
  vehicles: 'Lưu năng lực tải, kích thước thùng, loại xe và điều kiện giấy phép của phương tiện.',
  products: 'Lưu danh mục sản phẩm điện tử cùng kích thước, khối lượng và đặc tính đóng gói.',
  excel_imports: 'Theo dõi lịch sử import Excel, trạng thái xử lý và số dòng hợp lệ hoặc lỗi.',
  orders: 'Lưu đơn giao hàng theo cửa hàng, sản phẩm, ngày giao và trạng thái xử lý.',
  trips: 'Lưu chuyến giao hàng đã được lập kế hoạch cùng xe, tài xế, lịch chạy và tổng tải.',
  trip_stops: 'Lưu tiến trình của từng điểm dừng trong chuyến, gồm thứ tự, ETA, thời điểm thực tế và trạng thái.',
  loading_manifest_items: 'Lưu các mục hàng được xếp lên xe theo chuyến, điểm dừng và thứ tự chất hàng.',
  delivery_records: 'Lưu kết quả giao hàng tại điểm dừng, người thực hiện, trạng thái và ghi chú.',
  epod: 'Lưu bằng chứng giao hàng điện tử gồm chữ ký và hình ảnh hàng hóa.',
  exceptions: 'Lưu ngoại lệ phát sinh theo chuyến hoặc điểm dừng và trạng thái giải quyết.',
  trip_drafts: 'Lưu bản nháp chuyến được gom theo tuyến và ngày giao trước khi xác nhận vận hành.',
  trip_draft_stops: 'Lưu các điểm dừng dự kiến, số đơn và lựa chọn kích hoạt trong một trip draft.',
  system_config: 'Lưu cấu hình vận hành dạng khóa-giá trị như tọa độ kho và tốc độ trung bình.',
  manifests: 'Lưu phần đầu manifest, nguồn trip draft và tổng khối lượng, thể tích cần xếp.',
  manifest_lines: 'Lưu từng dòng hàng trên manifest với thứ tự LIFO, điểm dừng và snapshot sản phẩm.',
  delivery_exceptions: 'Lưu sự cố giao hàng có loại, người báo cáo, mô tả và thông tin xử lý.',
  administrative_regions: 'Lưu các vùng hành chính cấp cao và tên gọi song ngữ dùng làm dữ liệu tham chiếu.',
  administrative_units: 'Lưu danh mục loại đơn vị hành chính cùng tên đầy đủ, tên ngắn và mã chuẩn hóa.',
  provinces: 'Lưu tỉnh hoặc thành phố trực thuộc trung ương và liên kết đến vùng, cấp hành chính.',
  districts: 'Lưu quận, huyện hoặc thành phố cấp huyện và liên kết đến tỉnh tương ứng.',
  wards: 'Lưu xã, phường hoặc thị trấn và liên kết đến huyện tương ứng.',
  permissions: 'Lưu các quyền thao tác cấp chức năng được sử dụng trong mô hình RBAC.',
  role_permissions: 'Bảng nối gán nhiều quyền cho nhiều vai trò trong mô hình RBAC.',
  user_roles: 'Bảng nối nhiều-nhiều giữa tài khoản người dùng và vai trò.',
  import_batches: 'Đại diện batch import đơn hàng, ngày giao, tệp nguồn và thống kê chấp nhận hoặc từ chối.',
  order_items: 'Lưu từng dòng sản phẩm của đơn hàng với snapshot số lượng, khối lượng và thể tích.',
  import_errors: 'Lưu lỗi kiểm tra dữ liệu theo dòng, trường và mã lỗi của từng batch import.',
};

const tableSpecificTag = {
  roles: 'phân-quyền', users: 'người-dùng', stores: 'cửa-hàng', routes: 'tuyến-giao-hàng',
  route_stops: 'điểm-dừng', vehicles: 'phương-tiện', products: 'sản-phẩm', excel_imports: 'import-excel',
  orders: 'đơn-hàng', trips: 'chuyến-giao-hàng', trip_stops: 'tiến-trình-giao-hàng',
  loading_manifest_items: 'xếp-hàng', delivery_records: 'giao-hàng', epod: 'bằng-chứng-giao-hàng',
  exceptions: 'ngoại-lệ', trip_drafts: 'lập-kế-hoạch', trip_draft_stops: 'điểm-dừng-dự-kiến',
  system_config: 'cấu-hình-hệ-thống', manifests: 'manifest', manifest_lines: 'lifo',
  delivery_exceptions: 'ngoại-lệ-giao-hàng', administrative_regions: 'địa-giới-hành-chính',
  administrative_units: 'đơn-vị-hành-chính', provinces: 'tỉnh-thành', districts: 'quận-huyện',
  wards: 'xã-phường', permissions: 'rbac', role_permissions: 'rbac', user_roles: 'rbac',
  import_batches: 'import-đơn-hàng', order_items: 'chi-tiết-đơn-hàng', import_errors: 'lỗi-import',
};

const moderateTables = new Set([
  'users', 'stores', 'vehicles', 'products', 'orders', 'trips', 'trip_stops', 'loading_manifest_items',
  'delivery_records', 'trip_drafts', 'trip_draft_stops', 'manifests', 'manifest_lines',
  'provinces', 'districts', 'wards',
]);

const extraTablesByPath = {
  [P.v10]: ['import_batches', 'order_items', 'import_errors'],
};

function existingTable(filePath, tableName) {
  return tableId(filePath, tableName);
}

const manualRelationships = {
  [P.v10]: [
    ['migrates', existingTable(P.v1, 'orders')],
  ],
  [P.v11]: [
    ['migrates', existingTable(P.v10, 'import_errors')],
  ],
  [P.v12]: [
    ['migrates', existingTable(P.v1, 'orders')],
  ],
  [P.v13]: [
    ['migrates', existingTable(P.v1, 'orders')],
    ['migrates', existingTable(P.v12, 'trip_drafts')],
    ['migrates', existingTable(P.v12, 'trip_draft_stops')],
  ],
  [P.v14]: [
    ['migrates', existingTable(P.v1, 'route_stops')],
    ['migrates', existingTable(P.v12, 'trip_drafts')],
    ['migrates', existingTable(P.v12, 'trip_draft_stops')],
  ],
  [P.v15]: [
    ['migrates', existingTable(P.v12, 'trip_drafts')],
    ['depends_on', existingTable(P.v1, 'users')],
  ],
  [P.v16]: [
    ['migrates', existingTable(P.v12, 'trip_drafts')],
  ],
  [P.v17]: [
    ['migrates', existingTable(P.v1, 'loading_manifest_items')],
    ['migrates', existingTable(P.v1, 'delivery_records')],
    ['migrates', existingTable(P.v1, 'exceptions')],
  ],
  [P.v18]: [
    ['migrates', existingTable(P.v17, 'trips')],
    ['migrates', existingTable(P.v17, 'trip_stops')],
  ],
  [P.v2]: [
    ['migrates', existingTable(P.v1, 'roles')],
    ['migrates', existingTable(P.v1, 'users')],
    ['migrates', existingTable(P.v1, 'stores')],
    ['migrates', existingTable(P.v1, 'routes')],
    ['migrates', existingTable(P.v1, 'route_stops')],
    ['migrates', existingTable(P.v1, 'vehicles')],
    ['migrates', existingTable(P.v1, 'products')],
  ],
  [P.v20]: [
    ['migrates', existingTable(P.v1, 'stores')],
    ['depends_on', existingTable(P.v19, 'provinces')],
    ['depends_on', existingTable(P.v19, 'districts')],
    ['depends_on', existingTable(P.v19, 'wards')],
  ],
  [P.v21]: [
    ['migrates', existingTable(P.v17, 'trip_stops')],
  ],
  [P.v22]: [
    ['migrates', existingTable(P.v1, 'stores')],
    ['migrates', existingTable(P.v1, 'vehicles')],
    ['migrates', existingTable(P.v1, 'products')],
  ],
  [P.v23]: [
    ['migrates', existingTable(P.v10, 'import_batches')],
  ],
  [P.v24]: [
    ['migrates', existingTable(P.v1, 'orders')],
  ],
  [P.v25]: [
    ['migrates', existingTable(P.v1, 'vehicles')],
    ['migrates', existingTable(P.v1, 'users')],
  ],
  [P.v26]: [
    ['migrates', existingTable(P.v17, 'trip_stops')],
    ['migrates', existingTable(P.v17, 'trips')],
    ['migrates', existingTable(P.v16, 'manifest_lines')],
    ['migrates', existingTable(P.v16, 'manifests')],
    ['migrates', existingTable(P.v12, 'trip_draft_stops')],
    ['migrates', existingTable(P.v12, 'trip_drafts')],
    ['migrates', existingTable(P.v1, 'route_stops')],
    ['migrates', existingTable(P.v1, 'routes')],
    ['migrates', existingTable(P.v10, 'order_items')],
    ['migrates', existingTable(P.v1, 'orders')],
    ['migrates', existingTable(P.v10, 'import_errors')],
    ['migrates', existingTable(P.v10, 'import_batches')],
    ['migrates', existingTable(P.v1, 'stores')],
    ['migrates', existingTable(P.v1, 'vehicles')],
    ['migrates', existingTable(P.v1, 'products')],
    ['migrates', existingTable(P.v1, 'users')],
    ['migrates', existingTable(P.v3, 'user_roles')],
    ['migrates', existingTable(P.v14, 'system_config')],
  ],
  [P.v27]: [
    ['migrates', existingTable(P.v1, 'stores')],
    ['depends_on', existingTable(P.v19, 'districts')],
    ['depends_on', existingTable(P.v19, 'wards')],
  ],
  [P.v29]: [
    ['migrates', existingTable(P.v28, 'permissions')],
    ['migrates', existingTable(P.v28, 'role_permissions')],
  ],
  [P.v3]: [
    ['migrates', existingTable(P.v1, 'users')],
    ['migrates', existingTable(P.v1, 'roles')],
  ],
  [P.v30]: [
    ['migrates', existingTable(P.v1, 'stores')],
    ['migrates', existingTable(P.v12, 'trip_draft_stops')],
    ['migrates', existingTable(P.v17, 'trip_stops')],
  ],
  [P.v31]: [
    ['migrates', existingTable(P.v1, 'orders')],
  ],
};

function complexityFromLines(nonEmptyLines) {
  if (nonEmptyLines > 200) return 'complex';
  if (nonEmptyLines >= 50) return 'moderate';
  return 'simple';
}

function edgeWeight(type) {
  if (type === 'migrates') return 0.7;
  if (type === 'depends_on') return 0.6;
  throw new Error(`Loại edge không được hỗ trợ: ${type}`);
}

function build() {
  const extraction = JSON.parse(fs.readFileSync(extractPath, 'utf8'));
  if (extraction.scriptCompleted !== true || extraction.filesAnalyzed !== 25 || extraction.filesSkipped.length !== 0) {
    throw new Error('Kết quả extractor không bao phủ đủ 25 migration.');
  }

  const nodes = [];
  const edges = [];
  const edgeKeys = new Set();
  const resultByPath = new Map(extraction.results.map((result) => [result.path, result]));

  for (const filePath of Object.values(P)) {
    const result = resultByPath.get(filePath);
    const meta = migrationMeta[filePath];
    if (!result || !meta) throw new Error(`Thiếu metadata cho ${filePath}`);
    const node = {
      id: migrationId(filePath),
      type: 'table',
      name: path.posix.basename(filePath),
      filePath,
      summary: meta.summary,
      tags: meta.tags,
      complexity: complexityFromLines(result.nonEmptyLines),
    };
    if (meta.languageNotes) node.languageNotes = meta.languageNotes;
    nodes.push(node);
  }

  for (const result of extraction.results) {
    const tableNames = (result.definitions || [])
      .filter((definition) => definition.kind === 'table')
      .map((definition) => definition.name);
    tableNames.push(...(extraTablesByPath[result.path] || []));
    for (const tableName of [...new Set(tableNames)]) {
      nodes.push({
        id: tableId(result.path, tableName),
        type: 'table',
        name: tableName,
        filePath: result.path,
        summary: tableSummaries[tableName] || `Biểu diễn bảng ${tableName} được khai báo hoặc cập nhật trong migration này.`,
        tags: ['bảng-dữ-liệu', 'schema', tableSpecificTag[tableName] || 'cơ-sở-dữ-liệu'],
        complexity: moderateTables.has(tableName) ? 'moderate' : 'simple',
      });
      const key = `${migrationId(result.path)}|${tableId(result.path, tableName)}|migrates`;
      if (!edgeKeys.has(key)) {
        edgeKeys.add(key);
        edges.push({
          source: migrationId(result.path),
          target: tableId(result.path, tableName),
          type: 'migrates',
          direction: 'forward',
          weight: 0.7,
        });
      }
    }
  }

  for (const [filePath, relationships] of Object.entries(manualRelationships)) {
    for (const [type, target] of relationships) {
      const key = `${migrationId(filePath)}|${target}|${type}`;
      if (edgeKeys.has(key)) continue;
      edgeKeys.add(key);
      edges.push({
        source: migrationId(filePath),
        target,
        type,
        direction: 'forward',
        weight: edgeWeight(type),
      });
    }
  }

  const nodeIds = new Set(nodes.map((node) => node.id));
  if (nodeIds.size !== nodes.length) throw new Error('Batch có node ID trùng lặp.');
  for (const edge of edges) {
    if (!nodeIds.has(edge.source) || !nodeIds.has(edge.target)) {
      throw new Error(`Edge tham chiếu node không tồn tại: ${edge.source} -> ${edge.target}`);
    }
    if (edge.source === edge.target) throw new Error(`Edge tự tham chiếu: ${edge.source}`);
  }
  const migrationNodes = nodes.filter((node) => node.id.endsWith(':migration'));
  if (migrationNodes.length !== 25) throw new Error(`Thiếu migration node: chỉ có ${migrationNodes.length}/25.`);

  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify({ nodes, edges }, null, 2)}\n`, 'utf8');
}

try {
  build();
  process.exit(0);
} catch (error) {
  process.stderr.write(`${error && error.stack ? error.stack : error}\n`);
  process.exit(1);
}
