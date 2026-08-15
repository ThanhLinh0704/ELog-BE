#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');

const projectRoot = path.resolve(process.argv[2]);
const inputPath = path.resolve(process.argv[3]);
const extractPath = path.resolve(process.argv[4]);
const outputPath = path.resolve(process.argv[5]);
const migrationRoot = 'src/main/resources/db/migration';
const p = (name) => `${migrationRoot}/${name}`;
const sqlParentId = (filePath) => filePath.includes('/db/migration/')
  ? `table:${filePath}:migration`
  : `table:${filePath}:database-setup`;
const tableId = (filePath, tableName) => `table:${filePath}:${tableName}`;

const P = {
  v32: p('V32__driver_app_execution_and_outcomes_schema.sql'),
  v33: p('V33__add_returned_to_warehouse_at.sql'),
  v34: p('V34__add_assigned_driver_to_vehicles.sql'),
  v35: p('V35__allow_null_delivery_date_import_batches.sql'),
  v36: p('V36__add_goong_route_info_to_trip_drafts.sql'),
  v37: p('V37__driver_active_status_schema.sql'),
  v38: p('V38__two_vehicle_safety_buffer_config.sql'),
  v39: p('V39__planning_history_schema.sql'),
  v4: p('V4__add_refresh_tokens_table.sql'),
  v40: p('V40__trip_outcome_history_schema.sql'),
  v41: p('V41__add_kpi_permission.sql'),
  v42: p('V42__decouple_history_events_from_fk_constraints.sql'),
  v43: p('V43__seed_missing_system_user.sql'),
  v44: p('V44__extend_delivery_exceptions_for_driver_app.sql'),
  v45: p('V45__refresh_seed_from_excel_v1.sql'),
  v46: p('V46__make_trip_stops_route_stop_id_nullable.sql'),
  v47: p('V47__make_trip_stops_trip_draft_stop_id_nullable.sql'),
  v48: p('V48__delete_stores_kh0033_kh0035.sql'),
  v5: p('V5__add_store_contact_fields.sql'),
  v6: p('V6__add_route_description.sql'),
  v7: p('V7__alter_products_schema.sql'),
  v8: p('V8__vehicles_schema.sql'),
  v9: p('V9__orders_schema.sql'),
  setup: 'src/main/resources/db/setup/create_database.sql',
  claude: 'CLAUDE.md',
};

const X = {
  roles: tableId(p('V1__init_schema.sql'), 'roles'),
  users: tableId(p('V1__init_schema.sql'), 'users'),
  stores: tableId(p('V1__init_schema.sql'), 'stores'),
  routes: tableId(p('V1__init_schema.sql'), 'routes'),
  routeStops: tableId(p('V1__init_schema.sql'), 'route_stops'),
  vehicles: tableId(p('V1__init_schema.sql'), 'vehicles'),
  products: tableId(p('V1__init_schema.sql'), 'products'),
  excelImports: tableId(p('V1__init_schema.sql'), 'excel_imports'),
  orders: tableId(p('V1__init_schema.sql'), 'orders'),
  loadingManifestItems: tableId(p('V1__init_schema.sql'), 'loading_manifest_items'),
  tripDrafts: tableId(p('V12__trip_draft_schema.sql'), 'trip_drafts'),
  tripDraftStops: tableId(p('V12__trip_draft_schema.sql'), 'trip_draft_stops'),
  systemConfig: tableId(p('V14__trip_draft_review_fields.sql'), 'system_config'),
  manifests: tableId(p('V16__manifest_schema.sql'), 'manifests'),
  manifestLines: tableId(p('V16__manifest_schema.sql'), 'manifest_lines'),
  trips: tableId(p('V17__trip_tripstop_schema.sql'), 'trips'),
  tripStops: tableId(p('V17__trip_tripstop_schema.sql'), 'trip_stops'),
  deliveryExceptions: tableId(p('V18__progress_schema_update.sql'), 'delivery_exceptions'),
  permissions: tableId(p('V28__add_permissions_schema.sql'), 'permissions'),
  rolePermissions: tableId(p('V28__add_permissions_schema.sql'), 'role_permissions'),
  userRoles: tableId(p('V3__update_user_roles_and_email.sql'), 'user_roles'),
};

const meta = {
  [P.v32]: {
    summary: 'Tạo trip_executions, delivery_order_results và trip_outcomes để Driver App quản lý phiên thực thi chuyến, kết quả từng đơn và kết quả tổng hợp sau giao hàng.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'driver-app', 'kết-quả-giao-hàng'],
    languageNotes: 'Schema tách trạng thái thực thi chuyến, kết quả theo đơn và bản tổng hợp để mỗi lớp dữ liệu có vòng đời riêng nhưng vẫn liên kết bằng khóa ngoại.',
  },
  [P.v33]: {
    summary: 'Bổ sung thời điểm tài xế quay về kho cho trip_executions để hoàn thiện mốc kết thúc quy trình thực thi chuyến.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'driver-app', 'về-kho'],
  },
  [P.v34]: {
    summary: 'Gắn tài xế được phân công vào vehicles bằng khóa ngoại và index, sau đó backfill các cặp xe-tài xế phù hợp hạng bằng lái.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'phân-công-tài-xế', 'phương-tiện'],
  },
  [P.v35]: {
    summary: 'Cho phép delivery_date của import_batches nhận NULL để hỗ trợ các tệp import chưa xác định ngày giao.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'import-đơn-hàng', 'nullable'],
  },
  [P.v36]: {
    summary: 'Lưu quãng đường, encoded polyline và thời gian di chuyển từng chặng từ Goong API trên cả trip draft lẫn chuyến đã xác nhận.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'goong-api', 'định-tuyến', 'polyline'],
  },
  [P.v37]: {
    summary: 'Bổ sung trạng thái Active/Inactive của tài xế, bảng lịch sử thay đổi và quyền đọc/ghi để quản lý khả năng nhận chuyến.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'trạng-thái-tài-xế', 'audit', 'rbac'],
  },
  [P.v38]: {
    summary: 'Nạp tỷ lệ đệm an toàn 90% vào system_config để bộ máy đề xuất hai xe không sử dụng hết tuyệt đối tải trọng hoặc thể tích.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'safety-buffer', 'đề-xuất-xe'],
  },
  [P.v39]: {
    summary: 'Tạo trip_planning_events làm audit trail cho vòng đời lập kế hoạch chuyến và cấp quyền xem lịch sử cho các vai trò điều phối.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'audit-trail', 'lập-kế-hoạch-chuyến', 'rbac'],
    languageNotes: 'Bảng sự kiện lưu cả snapshot trạng thái, JSON change_detail và nhiều index theo đối tượng, loại sự kiện, tác nhân, tuyến và ngày giao.',
  },
  [P.v4]: {
    summary: 'Tạo bảng refresh_tokens liên kết người dùng, token, thời hạn và thời điểm phát hành để hỗ trợ vòng đời đăng nhập JWT.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'refresh-token', 'xác-thực', 'jwt'],
  },
  [P.v40]: {
    summary: 'Lưu mã lý do thất bại trên delivery_order_results và tạo trip_outcome_events để theo dõi toàn bộ lịch sử thay đổi kết quả chuyến.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'outcome-history', 'audit-trail', 'driver-app'],
    languageNotes: 'Event table giữ snapshot denormalized của đơn, cửa hàng, tài xế, tuyến và ngày giao để lịch sử vẫn đọc được độc lập với trạng thái hiện tại.',
  },
  [P.v41]: {
    summary: 'Tạo quyền kpi:read và gán cho System Admin, Dispatcher cùng Logistics Manager để truy cập dashboard chỉ số.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'kpi', 'phân-quyền', 'rbac'],
  },
  [P.v42]: {
    summary: 'Gỡ khóa ngoại từ các bảng lịch sử đến entity nghiệp vụ để audit transaction REQUIRES_NEW có thể ghi ID chưa commit và giữ log sau khi dữ liệu gốc bị xóa.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'audit-log', 'transaction-boundary', 'foreign-key'],
    languageNotes: 'Thiết kế cố ý ưu tiên tính bền vững của audit log hơn referential integrity cứng, trong khi vẫn giữ cột ID và index để truy vấn.',
  },
  [P.v43]: {
    summary: 'Khôi phục tài khoản system có ID cố định 1 và gán vai trò SYSTEM_ADMIN để các job tự động có actor hợp lệ khi tạo delivery exception.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'system-user', 'dữ-liệu-mẫu', 'phân-quyền'],
  },
  [P.v44]: {
    summary: 'Mở rộng delivery_exceptions cho Driver App bằng cách cho phép thiếu trip stop và thêm tham chiếu trip execution, order cùng index tra cứu.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'driver-app', 'ngoại-lệ-giao-hàng'],
  },
  [P.v45]: {
    summary: 'Làm mới bộ dữ liệu demo từ Excel V1: xóa dữ liệu vận hành cũ rồi nạp lại sản phẩm, phương tiện, tài xế, cửa hàng, tuyến và điểm dừng.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'dữ-liệu-mẫu', 'excel', 'reset-dữ-liệu'],
    languageNotes: 'Migration có tính phá hủy chủ động, TRUNCATE theo thứ tự phụ thuộc trước khi nạp lại hàng trăm bản ghi tham chiếu từ nguồn Excel.',
  },
  [P.v46]: {
    summary: 'Cho phép route_stop_id trên trip_stops nhận NULL để điểm dừng thực thi không bắt buộc phải xuất phát từ tuyến cố định.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'điểm-dừng', 'nullable'],
  },
  [P.v47]: {
    summary: 'Cho phép trip_draft_stop_id trên trip_stops nhận NULL để điểm dừng thực tế có thể tồn tại ngoài bản nháp chuyến ban đầu.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'điểm-dừng', 'nullable'],
  },
  [P.v48]: {
    summary: 'Xóa hai cửa hàng KH0033 và KH0035 sau khi dọn các route_stops và orders phụ thuộc để bảo toàn ràng buộc dữ liệu.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'dọn-dữ-liệu', 'cửa-hàng'],
  },
  [P.v5]: {
    summary: 'Bổ sung số điện thoại và email liên hệ cho stores để lưu đầu mối nhận hàng tại cửa hàng.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'cửa-hàng', 'thông-tin-liên-hệ'],
  },
  [P.v6]: {
    summary: 'Thêm mô tả cho routes, dấu thời gian cho route_stops và đổi sequence_no thành sequence_order để làm rõ thứ tự điểm dừng.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'tuyến-giao-hàng', 'điểm-dừng'],
  },
  [P.v7]: {
    summary: 'Chuẩn hóa products theo US-07 bằng SKU, tên sản phẩm, ràng buộc kích thước/khối lượng dương và bộ dữ liệu sản phẩm điện tử chuẩn.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'danh-mục-sản-phẩm', 'data-backfill', 'validation'],
  },
  [P.v8]: {
    summary: 'Chuyển vehicles từ schema placeholder sang master data US-06 với loại xe, tải trọng, thể tích, ràng buộc dương và dữ liệu xe mẫu.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'quản-lý-phương-tiện', 'data-backfill', 'validation'],
  },
  [P.v9]: {
    summary: 'Chuẩn hóa import đơn Excel theo mô hình batch → order → order items, tạo bảng lỗi và chuyển orders từ mỗi dòng một sản phẩm sang đơn nhiều sản phẩm.',
    tags: ['cơ-sở-dữ-liệu', 'migration', 'import-excel', 'chuẩn-hóa-đơn-hàng', 'schema'],
    languageNotes: 'Migration tách phần đầu đơn và dòng sản phẩm, lưu snapshot trọng lượng/thể tích, đồng thời thay excel_imports bằng import_batches có ràng buộc batch hoạt động.',
  },
  [P.setup]: {
    summary: 'Khởi tạo thủ công database elog_db với utf8mb4, chọn database và cung cấp mẫu tạo user ứng dụng trước khi Spring Boot khởi động.',
    tags: ['cơ-sở-dữ-liệu', 'mysql', 'khởi-tạo-database', 'thiết-lập'],
    languageNotes: 'Database dùng utf8mb4_unicode_ci để lưu nhất quán tiếng Việt và các ký tự Unicode.',
  },
  [P.claude]: {
    summary: 'Tài liệu ngữ cảnh cô đọng mô tả phạm vi ELog, tech stack, actor, vòng đời chuyến, business rules trọng yếu, cấu trúc thư mục và checklist hoàn thành.',
    tags: ['tài-liệu', 'nguồn-sự-thật', 'business-rules', 'hướng-dẫn-phát-triển'],
    languageNotes: 'Markdown được tổ chức theo progressive disclosure: giữ quy tắc cốt lõi trong một tệp ngắn và liên kết sang docs chuyên đề khi cần.',
  },
};

const tableSummary = {
  trip_executions: 'Lưu phiên thực thi của một chuyến, tài xế được giao, version phân công và các mốc bắt đầu, hoàn tất, về kho.',
  delivery_order_results: 'Lưu kết quả giao của từng đơn trong một trip execution cùng điểm dừng, trạng thái và lý do thất bại.',
  trip_outcomes: 'Lưu kết quả tổng hợp của trip execution, số đơn thành công, thất bại, một phần và trạng thái xác nhận.',
  driver_status_history: 'Lưu lịch sử chuyển trạng thái Active/Inactive của tài xế, lý do và người thực hiện thay đổi.',
  trip_planning_events: 'Lưu audit event của quá trình lập trip draft và trip, gồm actor, trạng thái trước/sau và chi tiết thay đổi.',
  refresh_tokens: 'Lưu refresh token theo người dùng với thời hạn và thời điểm tạo phục vụ xác thực JWT.',
  trip_outcome_events: 'Lưu audit event của kết quả chuyến và đơn giao với snapshot nghiệp vụ tại thời điểm phát sinh.',
  import_batches: 'Lưu metadata tệp import, ngày giao, thống kê dòng hợp lệ/lỗi và trạng thái xử lý batch.',
  order_items: 'Lưu các sản phẩm thuộc đơn cùng snapshot SKU, số lượng, khối lượng và thể tích tại lúc import.',
  import_errors: 'Lưu dữ liệu thô và lý do lỗi theo từng dòng của batch import.',
};

const tableTag = {
  trip_executions: 'thực-thi-chuyến', delivery_order_results: 'kết-quả-đơn-giao', trip_outcomes: 'kết-quả-chuyến',
  driver_status_history: 'trạng-thái-tài-xế', trip_planning_events: 'audit-lập-kế-hoạch', refresh_tokens: 'xác-thực',
  trip_outcome_events: 'audit-kết-quả', import_batches: 'import-đơn-hàng', order_items: 'chi-tiết-đơn-hàng', import_errors: 'lỗi-import',
};

const moderateTables = new Set(['trip_executions', 'delivery_order_results', 'trip_outcomes', 'trip_planning_events', 'trip_outcome_events', 'import_batches', 'order_items']);

const R = {
  tripExecutions: tableId(P.v32, 'trip_executions'),
  deliveryOrderResults: tableId(P.v32, 'delivery_order_results'),
  tripOutcomes: tableId(P.v32, 'trip_outcomes'),
  driverStatusHistory: tableId(P.v37, 'driver_status_history'),
  planningEvents: tableId(P.v39, 'trip_planning_events'),
  refreshTokens: tableId(P.v4, 'refresh_tokens'),
  outcomeEvents: tableId(P.v40, 'trip_outcome_events'),
  importBatches: tableId(P.v9, 'import_batches'),
  orderItems: tableId(P.v9, 'order_items'),
  importErrors: tableId(P.v9, 'import_errors'),
};

const relations = {
  [P.v32]: [['depends_on', X.trips], ['depends_on', X.users], ['depends_on', X.orders], ['depends_on', X.tripDraftStops]],
  [P.v33]: [['migrates', R.tripExecutions]],
  [P.v34]: [['migrates', X.vehicles], ['depends_on', X.users]],
  [P.v35]: [['migrates', R.importBatches]],
  [P.v36]: [['migrates', X.tripDrafts], ['migrates', X.tripDraftStops], ['migrates', X.trips], ['migrates', X.tripStops]],
  [P.v37]: [['migrates', X.users], ['migrates', X.permissions], ['migrates', X.rolePermissions]],
  [P.v38]: [['migrates', X.systemConfig]],
  [P.v39]: [['migrates', X.permissions], ['migrates', X.rolePermissions], ['depends_on', X.tripDrafts], ['depends_on', X.trips], ['depends_on', X.users]],
  [P.v4]: [['depends_on', X.users]],
  [P.v40]: [['migrates', R.deliveryOrderResults], ['depends_on', R.tripExecutions], ['depends_on', X.trips], ['depends_on', X.users]],
  [P.v41]: [['migrates', X.permissions], ['migrates', X.rolePermissions]],
  [P.v42]: [['migrates', R.planningEvents], ['migrates', R.outcomeEvents]],
  [P.v43]: [['migrates', X.users], ['migrates', X.userRoles], ['depends_on', X.roles]],
  [P.v44]: [['migrates', X.deliveryExceptions], ['depends_on', R.tripExecutions], ['depends_on', X.orders]],
  [P.v45]: [
    ['migrates', R.driverStatusHistory], ['migrates', R.outcomeEvents], ['migrates', R.tripOutcomes],
    ['migrates', X.deliveryExceptions], ['migrates', R.tripExecutions], ['migrates', R.planningEvents],
    ['migrates', X.tripStops], ['migrates', X.trips], ['migrates', X.manifestLines], ['migrates', X.manifests],
    ['migrates', X.tripDraftStops], ['migrates', X.tripDrafts], ['migrates', X.routeStops], ['migrates', X.routes],
    ['migrates', R.orderItems], ['migrates', X.orders], ['migrates', R.importErrors], ['migrates', R.importBatches],
    ['migrates', X.stores], ['migrates', X.vehicles], ['migrates', X.products], ['migrates', X.users],
    ['migrates', X.userRoles], ['migrates', X.systemConfig],
  ],
  [P.v46]: [['migrates', X.tripStops]],
  [P.v47]: [['migrates', X.tripStops]],
  [P.v48]: [['migrates', X.routeStops], ['migrates', X.orders], ['migrates', X.stores]],
  [P.v5]: [['migrates', X.stores]],
  [P.v6]: [['migrates', X.routes], ['migrates', X.routeStops]],
  [P.v7]: [['migrates', X.products]],
  [P.v8]: [['migrates', X.vehicles]],
  [P.v9]: [
    ['migrates', X.orders], ['migrates', X.loadingManifestItems], ['migrates', X.excelImports],
    ['depends_on', X.users], ['depends_on', X.products],
  ],
  [P.claude]: [['documents', 'file:src/main/java/com/elog/ElogApplication.java']],
};

const weights = { migrates: 0.7, depends_on: 0.6, documents: 0.5 };

function complexity(nonEmptyLines) {
  if (nonEmptyLines > 200) return 'complex';
  if (nonEmptyLines >= 50) return 'moderate';
  return 'simple';
}

function parentId(file) {
  return file.fileCategory === 'docs' ? `document:${file.path}` : sqlParentId(file.path);
}

function build() {
  const input = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
  const extraction = JSON.parse(fs.readFileSync(extractPath, 'utf8'));
  if (input.batchFiles.length !== 25 || Object.values(input.batchImportData).flat().length !== 0) throw new Error('Input batch 2 không hợp lệ.');
  if (extraction.scriptCompleted !== true || extraction.filesAnalyzed !== 25 || extraction.filesSkipped.length !== 0) throw new Error('Extractor không bao phủ đủ batch 2.');
  const resultByPath = new Map(extraction.results.map((result) => [result.path, result]));
  const nodes = [];
  const edges = [];
  const edgeKeys = new Set();

  for (const file of input.batchFiles) {
    const result = resultByPath.get(file.path);
    const fileMeta = meta[file.path];
    if (!result || !fileMeta) throw new Error(`Thiếu metadata: ${file.path}`);
    const node = {
      id: parentId(file),
      type: file.fileCategory === 'docs' ? 'document' : 'table',
      name: path.posix.basename(file.path),
      filePath: file.path,
      summary: fileMeta.summary,
      tags: fileMeta.tags,
      complexity: complexity(result.nonEmptyLines),
    };
    if (fileMeta.languageNotes) node.languageNotes = fileMeta.languageNotes;
    nodes.push(node);
  }

  for (const result of extraction.results) {
    if (result.fileCategory !== 'data') continue;
    for (const definition of result.definitions || []) {
      if (definition.kind !== 'table') continue;
      nodes.push({
        id: tableId(result.path, definition.name),
        type: 'table',
        name: definition.name,
        filePath: result.path,
        summary: tableSummary[definition.name] || `Biểu diễn bảng ${definition.name} được migration này khai báo.`,
        tags: ['bảng-dữ-liệu', 'schema', tableTag[definition.name] || 'cơ-sở-dữ-liệu'],
        complexity: moderateTables.has(definition.name) ? 'moderate' : 'simple',
      });
      const key = `${sqlParentId(result.path)}|${tableId(result.path, definition.name)}|migrates`;
      edgeKeys.add(key);
      edges.push({
        source: sqlParentId(result.path),
        target: tableId(result.path, definition.name),
        type: 'migrates',
        direction: 'forward',
        weight: 0.7,
      });
    }
  }

  for (const [filePath, fileRelations] of Object.entries(relations)) {
    const sourceFile = input.batchFiles.find((file) => file.path === filePath);
    if (!sourceFile) throw new Error(`Relationship source ngoài batch: ${filePath}`);
    for (const [type, target] of fileRelations) {
      const source = parentId(sourceFile);
      const key = `${source}|${target}|${type}`;
      if (edgeKeys.has(key)) continue;
      edgeKeys.add(key);
      edges.push({ source, target, type, direction: 'forward', weight: weights[type] });
    }
  }

  const ids = new Set(nodes.map((node) => node.id));
  if (ids.size !== nodes.length) throw new Error('Node ID trùng trong batch 2.');
  const batchOnePath = path.join(projectRoot, '.understand-anything', 'intermediate', 'batch-1.json');
  const batchOneIds = fs.existsSync(batchOnePath)
    ? new Set(JSON.parse(fs.readFileSync(batchOnePath, 'utf8')).nodes.map((node) => node.id))
    : new Set();
  for (const edge of edges) {
    const targetIsEntry = edge.target === 'file:src/main/java/com/elog/ElogApplication.java'
      && fs.existsSync(path.join(projectRoot, 'src/main/java/com/elog/ElogApplication.java'));
    if (!ids.has(edge.source)) throw new Error(`Edge source không tồn tại: ${edge.source}`);
    if (!ids.has(edge.target) && !batchOneIds.has(edge.target) && !targetIsEntry) throw new Error(`Edge target chưa được biết: ${edge.target}`);
    if (edge.source === edge.target) throw new Error(`Edge tự tham chiếu: ${edge.source}`);
  }

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
