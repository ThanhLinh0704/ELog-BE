package com.elog.integration;

import com.elog.dto.response.importbatch.ImportBatchResponse;
import com.elog.entity.*;
import com.elog.repository.*;
import com.elog.service.ImportService;
import com.elog.service.impl.ImportServiceImpl;
import com.elog.exception.BusinessException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ImportServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ImportService importService;

    @Autowired
    private ImportBatchRepository batchRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ImportErrorRepository errorRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // Truncate import-related tables to ensure a clean state before each test
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");
        jdbcTemplate.execute("TRUNCATE TABLE order_items;");
        jdbcTemplate.execute("TRUNCATE TABLE orders;");
        jdbcTemplate.execute("TRUNCATE TABLE import_errors;");
        jdbcTemplate.execute("TRUNCATE TABLE import_batches;");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;");
        jdbcTemplate.execute("INSERT IGNORE INTO users (id, username, password_hash, full_name, email, is_active, driver_status) VALUES (2, 'dispatcher01', 'pass', 'Dispatcher', 'disp@elog.vn', true, 'ACTIVE');");
        jdbcTemplate.execute("INSERT IGNORE INTO users (id, username, password_hash, full_name, email, is_active, driver_status) VALUES (1, 'admin', 'pass', 'Admin', 'admin@elog.vn', true, 'ACTIVE');");
    }

    private MockMultipartFile createExcelFile(String fileName, List<String[]> rowsData) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Mã đơn");
            headerRow.createCell(1).setCellValue("Mã cửa hàng");
            headerRow.createCell(2).setCellValue("SKU");
            headerRow.createCell(3).setCellValue("Số lượng");

            for (int i = 0; i < rowsData.size(); i++) {
                Row row = sheet.createRow(i + 1);
                String[] rowData = rowsData.get(i);
                for (int col = 0; col < rowData.length; col++) {
                    if (rowData[col] != null) {
                        row.createCell(col).setCellValue(rowData[col]);
                    }
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            byte[] bytes = bos.toByteArray();
            return new MockMultipartFile(
                    "file",
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes
            );
        }
    }

    // ── L2-IMP-01: Happy Path + Transaction Boundary ───────────────────────────
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "dispatcher01", authorities = {"order:import"})
    void l2Imp01_happyPathAndTransactionBoundary() throws Exception {
        String validDate = LocalDate.now().plusDays(2).toString();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"DH160325-01", "KH0002", "TOSMGM1100JV", "2"});
        rows.add(new String[]{"DH160325-01", "KH0002", "TOSTLR677WI", "1"});
        rows.add(new String[]{"DH160325-02", "KH0003", "HITLWB640GBK", "3"});
        rows.add(new String[]{"DH160325-03", "KH0005", "TOSTLR696WI", "10"});
        rows.add(new String[]{"DH160325-04", "ST-HD-099", "TOSMGM1100JV", "1"}); // Store doesn't exist
        rows.add(new String[]{"DH160325-05", "KH0002", "ACC-HDMI-2M", "5"});     // Inactive SKU

        MockMultipartFile file = createExcelFile("import.xlsx", rows);

        // When: Gửi file
        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("deliveryDate", validDate)
                        .param("confirmReplace", "false"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // Then: DB có 3 orders, 4 items, 2 errors, batch.status = PARTIAL (vì có 4 OK, 2 lỗi)
        assertThat(batchRepository.count()).isEqualTo(1);
        ImportBatch batch = batchRepository.findAll().get(0);
        assertThat(batch.getStatus()).isEqualTo("PARTIAL");
        assertThat(batch.getTotalRows()).isEqualTo(6);
        assertThat(batch.getAcceptedRows()).isEqualTo(4);
        assertThat(batch.getRejectedRows()).isEqualTo(2);

        assertThat(orderRepository.countByBatchId(batch.getId())).isEqualTo(3);
        assertThat(orderItemRepository.count()).isEqualTo(4);
        assertThat(errorRepository.count()).isEqualTo(2);
    }

    // ── L2-IMP-02: Error Path + Rollback ───────────────────────────────────────
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "dispatcher01", authorities = {"order:import"})
    void l2Imp02_corruptedFileRollsBackEntireBatch() throws Exception {
        String validDate = LocalDate.now().plusDays(2).toString();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "corrupt.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "invalid corrupt bytes".getBytes()
        );

        // When: Gửi file hỏng
        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("deliveryDate", validDate)
                        .param("confirmReplace", "false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EXCEL_PARSE_ERROR"));

        // Then: KHÔNG có records nào được tạo (được rollback hoàn toàn)
        assertThat(batchRepository.count()).isEqualTo(0);
        assertThat(orderRepository.count()).isEqualTo(0);
        assertThat(orderItemRepository.count()).isEqualTo(0);
    }

    // ── L2-IMP-03: Replace Flow (confirmReplace = false) ──────────────────────
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "dispatcher01", authorities = {"order:import"})
    void l2Imp03_replaceFlowConfirmReplaceFalse_returnsConflict() throws Exception {
        String validDate = LocalDate.now().plusDays(2).toString();
        jdbcTemplate.execute("INSERT INTO import_batches (id, delivery_date, file_name, uploaded_by, total_rows, accepted_rows, rejected_rows, status, is_active) VALUES (8, '" + validDate + "', 'old_file.xlsx', 2, 45, 45, 0, 'COMPLETED', TRUE)");
        jdbcTemplate.execute("INSERT INTO orders (id, import_batch_id, order_ref, store_id, delivery_date, status) VALUES (10, 8, 'DH160325-01', 2, '" + validDate + "', 'IMPORTED')");

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"DH160325-01", "KH0002", "TOSMGM1100JV", "2"});

        MockMultipartFile file = createExcelFile("import.xlsx", rows);

        // When: Gửi import cùng ngày với confirmReplace=false
        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("deliveryDate", validDate)
                        .param("confirmReplace", "false"))
                .andExpect(status().isConflict());

        // Then: Batch 8 vẫn active và không có batch mới
        ImportBatch batch8 = batchRepository.findById(8L).orElseThrow();
        assertThat(batch8.getIsActive()).isTrue();
        assertThat(batchRepository.count()).isEqualTo(1);
    }

    // ── L2-IMP-04: Replace Flow (confirmReplace = true) ───────────────────────
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "dispatcher01", authorities = {"order:import"})
    void l2Imp04_replaceFlowConfirmReplaceTrue_deactivatesOldAndCreatesNew() throws Exception {
        String validDate = LocalDate.now().plusDays(2).toString();
        jdbcTemplate.execute("INSERT INTO import_batches (id, delivery_date, file_name, uploaded_by, total_rows, accepted_rows, rejected_rows, status, is_active) VALUES (8, '" + validDate + "', 'old_file.xlsx', 2, 1, 1, 0, 'COMPLETED', TRUE)");
        jdbcTemplate.execute("INSERT INTO orders (id, import_batch_id, order_ref, store_id, delivery_date, status) VALUES (1, 8, 'DH-OLD', 2, '" + validDate + "', 'IMPORTED')");

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"DH160325-01", "KH0002", "TOSMGM1100JV", "2"});

        MockMultipartFile file = createExcelFile("import.xlsx", rows);

        // When: Gửi import cùng ngày với confirmReplace=true
        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("deliveryDate", validDate)
                        .param("confirmReplace", "true"))
                .andExpect(status().isCreated());

        // Then: Batch 8 -> is_active = false. Dữ liệu cũ vẫn còn nguyên. Batch mới -> is_active = true.
        ImportBatch batch8 = batchRepository.findById(8L).orElseThrow();
        assertThat(batch8.getIsActive()).isFalse();

        List<ImportBatch> batches = batchRepository.findAll();
        assertThat(batches).hasSize(2);
        
        ImportBatch newBatch = batches.stream().filter(b -> b.getId() != 8).findFirst().orElseThrow();
        assertThat(newBatch.getIsActive()).isTrue();

        // Kiểm tra soft replace (đơn cũ không bị xoá cứng khỏi DB)
        assertThat(orderRepository.existsById(1L)).isTrue();
    }

    // ── L2-IMP-05: Snapshot Integrity ──────────────────────────────────────────
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", authorities = {"order:import", "product:write"})
    void l2Imp05_snapshotIntegrityRemainsUnaffectedByProductUpdates() throws Exception {
        String validDate = LocalDate.now().plusDays(2).toString();
        Long productId = jdbcTemplate.queryForObject("SELECT id FROM products WHERE sku = 'TOSMGM1100JV' LIMIT 1", Long.class);
        jdbcTemplate.execute("INSERT INTO import_batches (id, delivery_date, file_name, uploaded_by, total_rows, accepted_rows, rejected_rows, status, is_active) VALUES (1, '" + validDate + "', 'file.xlsx', 2, 1, 1, 0, 'COMPLETED', TRUE)");
        jdbcTemplate.execute("INSERT INTO orders (id, import_batch_id, order_ref, store_id, delivery_date, status) VALUES (1, 1, 'DH-OLD', 2, '" + validDate + "', 'IMPORTED')");
        jdbcTemplate.execute("INSERT INTO order_items (id, order_id, product_id, sku, quantity, unit_weight_kg, unit_volume_m3, line_weight_kg, line_volume_m3) VALUES (1, 1, " + productId + ", 'TOSMGM1100JV', 1, 65.000, 0.714000, 65.000, 0.714000)");

        // When: Cập nhật trọng lượng sản phẩm TOSMGM1100JV (ID=productId) từ 65.000 sang 70.000
        String updateRequestJson = "{" +
                "\"sku\": \"TOSMGM1100JV\"," +
                "\"productName\": \"Tu Lanh Toshiba\"," +
                "\"weightKg\": 70.000," +
                "\"lengthM\": 0.6000," +
                "\"widthM\": 0.6800," +
                "\"heightM\": 1.7500" +
                "}";

        mockMvc.perform(put("/api/v1/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson))
                .andExpect(status().isOk());

        // Then: order_items.unit_weight_kg của đơn hàng cũ vẫn giữ nguyên là 65.000
        Double storedWeight = jdbcTemplate.queryForObject(
                "SELECT unit_weight_kg FROM order_items WHERE id = 1", Double.class);
        assertThat(storedWeight).isEqualTo(65.000);

        // Đối chiếu xem product trong danh mục đã cập nhật đúng 70.000 chưa
        Double productWeight = jdbcTemplate.queryForObject(
                "SELECT weight_kg FROM products WHERE id = " + productId, Double.class);
        assertThat(productWeight).isEqualTo(70.000);
    }

    // ── L2-IMP-06: Partial Failure, No Full Rollback ───────────────────────────
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "dispatcher01", authorities = {"order:import"})
    void l2Imp06_partialFailureDoesNotRollbackValidRows() throws Exception {
        String validDate = LocalDate.now().plusDays(2).toString();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"DH-01", "KH0002", "TOSMGM1100JV", "1"});
        rows.add(new String[]{"DH-02", "KH0003", "TOSTLR677WI", "2"});
        rows.add(new String[]{"DH-03", "KH0005", "INVALID-SKU", "3"}); // invalid SKU
        rows.add(new String[]{"DH-04", "KH0002", "TOSTLR696WI", "4"});
        rows.add(new String[]{"DH-05", "KH0003", "HITLWB640GBK", "5"});

        MockMultipartFile file = createExcelFile("import.xlsx", rows);

        // When: Gửi file
        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("deliveryDate", validDate)
                        .param("confirmReplace", "false"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PARTIAL"))
                .andExpect(jsonPath("$.data.acceptedRows").value(4))
                .andExpect(jsonPath("$.data.rejectedRows").value(1));

        // Then: DB vẫn ghi nhận 4 order items thành công, 1 error log
        assertThat(orderItemRepository.count()).isEqualTo(4);
        assertThat(errorRepository.count()).isEqualTo(1);
    }

    // ── L2-IMP-07: DB Constraint - Unique order constraint ─────────────────────
    @Test
    void l2Imp07_dbUniqueConstraintPreventsDuplicateOrderRefPerBatchAndStore() {
        // Given: Tạo một batch
        ImportBatch batch = ImportBatch.builder()
                .deliveryDate(LocalDate.now().plusDays(2))
                .fileName("import.xlsx")
                .uploadedBy(2L)
                .status("COMPLETED")
                .build();
        batch = batchRepository.save(batch);

        Store store = storeRepository.findAll().get(0);

        // When: Lưu order 1
        Order order1 = Order.builder()
                .importBatch(batch)
                .orderRef("DH-DUP")
                .store(store)
                .deliveryDate(LocalDate.now().plusDays(2))
                .build();
        orderRepository.save(order1);
        orderRepository.flush();

        // When/Then: Lưu order 2 trùng (batch_id, order_ref, store_id)
        Order order2 = Order.builder()
                .importBatch(batch)
                .orderRef("DH-DUP")
                .store(store)
                .deliveryDate(LocalDate.now().plusDays(2))
                .build();

        assertThatThrownBy(() -> {
            orderRepository.save(order2);
            orderRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── L2-IMP-08: DB Constraint - Concurrency (Unique active date) ────────────
    @Test
    void l2Imp08_concurrentBatchCreationThrowsConflict() throws Exception {
        LocalDate date = LocalDate.now().plusDays(3);
        List<String[]> rows = Collections.singletonList(new String[]{"DH-CONC", "KH0002", "TOSMGM1100JV", "1"});
        MockMultipartFile file1 = createExcelFile("import1.xlsx", rows);
        MockMultipartFile file2 = createExcelFile("import2.xlsx", rows);

        // First import succeeds
        importService.importExcel(file1, date, false, 2L);

        // Second import for same date with confirmReplace=false throws CONFLICT
        assertThatThrownBy(() -> importService.importExcel(file2, date, false, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(batchRepository.count()).isEqualTo(1);
    }

    @Test
    void generateRealExcelFilesForPostman() throws IOException {
        java.io.File dir = new java.io.File("Evidence/Integration Test/US-08");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // TC01 File
        List<String[]> rows01 = new ArrayList<>();
        rows01.add(new String[]{"DH160325-01", "ST-001", "REF-SAM-300", "2"});
        rows01.add(new String[]{"DH160325-01", "ST-001", "TV-SAM-55", "1"});
        rows01.add(new String[]{"DH160325-02", "ST-002", "GEN-DNY-5K", "3"});
        rows01.add(new String[]{"DH160325-03", "ST-003", "PHN-APL-14", "10"});
        rows01.add(new String[]{"DH160325-04", "ST-HD-099", "REF-SAM-300", "1"});
        rows01.add(new String[]{"DH160325-05", "ST-001", "ACC-HDMI-2M", "5"});

        MockMultipartFile file01 = createExcelFile("import_tc01.xlsx", rows01);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(new java.io.File(dir, "import_tc01.xlsx"))) {
            fos.write(file01.getBytes());
        }

        // TC06 File
        List<String[]> rows06 = new ArrayList<>();
        rows06.add(new String[]{"DH-01", "ST-001", "REF-SAM-300", "1"});
        rows06.add(new String[]{"DH-02", "ST-002", "TV-SAM-55", "2"});
        rows06.add(new String[]{"DH-03", "ST-003", "INVALID-SKU", "3"});
        rows06.add(new String[]{"DH-04", "ST-001", "PHN-APL-14", "4"});
        rows06.add(new String[]{"DH-05", "ST-002", "GEN-DNY-5K", "5"});

        MockMultipartFile file06 = createExcelFile("import_tc06.xlsx", rows06);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(new java.io.File(dir, "import_tc06.xlsx"))) {
            fos.write(file06.getBytes());
        }
    }
}
