package com.elog.integration;

import com.elog.dto.response.ImportBatchResponse;
import com.elog.service.ImportService;
import com.elog.service.impl.AutoPipelineRunner;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class ImportServiceReport5IntegrationTest {

    private static final String STORE = "KH0001";
    private static final String SKU = "TOSTLR677WI";

    @Autowired ImportService importService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @MockBean AutoPipelineRunner autoPipelineRunner;

    @Test
    void l2Imp01ValidWorkbookCommitsBatchOrdersAndItems() throws Exception {
        LocalDate date = LocalDate.of(2040, 1, 1);

        ImportBatchResponse response = importService.importExcel(
                workbook(List.<String[]>of(row("IMP01-A", STORE, SKU, "2"))), date, false, 3L);
        entityManager.flush();
        entityManager.clear();

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getAcceptedRows()).isEqualTo(1);
        assertThat(response.getRejectedRows()).isZero();
        assertThat(count("SELECT COUNT(*) FROM orders WHERE import_batch_id = ?", response.getBatchId())).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(*) FROM order_items oi JOIN orders o ON o.id = oi.order_id
                WHERE o.import_batch_id = ?
                """, response.getBatchId())).isEqualTo(1);
    }

    @Test
    void l2Imp02MixedWorkbookPersistsAcceptedRowsAndOneError() throws Exception {
        LocalDate date = LocalDate.of(2040, 1, 2);
        int ordersBefore = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
        List<String[]> rows = List.of(
                row("IMP02-A", STORE, SKU, "1"),
                row("IMP02-B", "UNKNOWN-STORE", SKU, "1"));

        ImportBatchResponse response = importService.importExcel(workbook(rows), date, false, 3L);
        entityManager.flush();

        assertThat(response.getAcceptedRows()).isEqualTo(1);
        assertThat(response.getRejectedRows()).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM orders WHERE import_batch_id = ?", response.getBatchId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM import_errors WHERE import_batch_id = ?", response.getBatchId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isEqualTo(ordersBefore + 1);
    }

    @Test
    void l2Imp03ConfirmReplaceDeactivatesPriorBatchAndActivatesNewBatch() throws Exception {
        LocalDate date = LocalDate.of(2040, 1, 3);
        jdbc.update("""
                INSERT INTO import_batches
                    (delivery_date, file_name, uploaded_by, total_rows, accepted_rows,
                     rejected_rows, status, is_active)
                VALUES (?, 'old.xlsx', 3, 1, 1, 0, 'COMPLETED', 1)
                """, date);
        long oldBatchId = lastInsertId();

        ImportBatchResponse response = importService.importExcel(
                workbook(List.<String[]>of(row("IMP03-A", STORE, SKU, "1"))), date, true, 3L);
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT is_active FROM import_batches WHERE id = ?",
                Boolean.class, oldBatchId)).isFalse();
        assertThat(jdbc.queryForObject("SELECT is_active FROM import_batches WHERE id = ?",
                Boolean.class, response.getBatchId())).isTrue();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void l2Imp04DuplicateProductRowsAccumulateIntoOneOrderItem() throws Exception {
        LocalDate date = LocalDate.of(2040, 1, 4);
        List<String[]> rows = List.of(
                row("IMP04-A", STORE, SKU, "5"),
                row("IMP04-A", STORE, SKU, "5"));

        ImportBatchResponse response = importService.importExcel(workbook(rows), date, false, 3L);
        entityManager.flush();
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT oi.quantity FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE o.import_batch_id = ?
                """, response.getBatchId());

        assertThat(count("SELECT COUNT(*) FROM orders WHERE import_batch_id = ?", response.getBatchId())).isEqualTo(1);
        assertThat(items).hasSize(1);
        assertThat(((Number) items.getFirst().get("quantity")).intValue()).isEqualTo(10);
    }

    @Test
    void l2Imp05RejectedExcelRowPersistsExactRowNumber() throws Exception {
        LocalDate date = LocalDate.of(2040, 1, 5);
        List<String[]> rows = List.of(
                row("IMP05-A", STORE, SKU, "1"),
                row("IMP05-B", "UNKNOWN-STORE", SKU, "1"));

        ImportBatchResponse response = importService.importExcel(workbook(rows), date, false, 3L);
        entityManager.flush();
        Map<String, Object> error = jdbc.queryForMap("""
                SELECT row_num, error_code, field_name
                FROM import_errors WHERE import_batch_id = ?
                """, response.getBatchId());

        assertThat(response.getAcceptedRows()).isEqualTo(1);
        assertThat(response.getRejectedRows()).isEqualTo(1);
        assertThat(((Number) error.get("row_num")).intValue()).isEqualTo(3);
        assertThat(error.get("error_code")).isEqualTo("STORE_NOT_FOUND");
        assertThat(error.get("field_name")).isEqualTo("storeCode");
    }

    private MultipartFile workbook(List<String[]> data) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("orders");
            Row header = sheet.createRow(0);
            String[] headers = {"orderRef", "storeCode", "sku", "quantity", "deliveryDate",
                    "deliveryTimeWindow", "recipientName", "recipientPhone", "notes"};
            for (int column = 0; column < headers.length; column++) {
                header.createCell(column).setCellValue(headers[column]);
            }
            for (int index = 0; index < data.size(); index++) {
                Row excelRow = sheet.createRow(index + 1);
                for (int column = 0; column < data.get(index).length; column++) {
                    excelRow.createCell(column).setCellValue(data.get(index)[column]);
                }
            }
            workbook.write(output);
            return new MockMultipartFile("file", "orders.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private String[] row(String ref, String store, String sku, String quantity) {
        List<String> values = new ArrayList<>(List.of(ref, store, sku, quantity));
        while (values.size() < 9) values.add("");
        return values.toArray(String[]::new);
    }

    private long lastInsertId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private int count(String sql, long value) {
        return jdbc.queryForObject(sql, Integer.class, value);
    }
}
