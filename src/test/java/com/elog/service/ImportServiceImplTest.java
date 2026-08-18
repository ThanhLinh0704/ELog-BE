package com.elog.service;

import com.elog.dto.response.importbatch.ImportBatchResponse;
import com.elog.entity.ImportBatch;
import com.elog.entity.ImportError;
import com.elog.entity.Order;
import com.elog.entity.OrderItem;
import com.elog.entity.Product;
import com.elog.entity.Store;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.ImportBatchRepository;
import com.elog.repository.ImportErrorRepository;
import com.elog.repository.OrderItemRepository;
import com.elog.repository.OrderRepository;
import com.elog.repository.ProductRepository;
import com.elog.repository.StoreRepository;
import com.elog.repository.TripDraftRepository;
import com.elog.repository.UserRepository;
import com.elog.service.impl.AutoPipelineRunner;
import com.elog.service.impl.ImportServiceImpl;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportServiceImplTest {
    private ImportBatchRepository batches;
    private OrderRepository orders;
    private OrderItemRepository items;
    private ImportErrorRepository errors;
    private StoreRepository stores;
    private ProductRepository products;
    private ImportServiceImpl service;
    private final LocalDate deliveryDate = LocalDate.now().plusDays(2);
    private Store store;
    private Product product;

    @BeforeEach
    void setUp() {
        batches = mock(ImportBatchRepository.class);
        orders = mock(OrderRepository.class);
        items = mock(OrderItemRepository.class);
        errors = mock(ImportErrorRepository.class);
        stores = mock(StoreRepository.class);
        products = mock(ProductRepository.class);
        service = new ImportServiceImpl(batches, orders, items, errors, stores, products,
                mock(UserRepository.class), mock(TripDraftRepository.class), mock(AutoPipelineRunner.class));
        store = Store.builder().id(1L).code("ST-001").name("Store 1").isActive(true).build();
        product = Product.builder().id(2L).sku("SKU-A").productName("Product A").isActive(true)
                .weightKg(BigDecimal.ONE).volumeM3(new BigDecimal("0.100000")).build();
        when(batches.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch batch = invocation.getArgument(0);
            if (batch.getId() == null) batch.setId(100L);
            return batch;
        });
        when(stores.findByCode("ST-001")).thenReturn(Optional.of(store));
        when(products.findBySku("SKU-A")).thenReturn(Optional.of(product));
        AtomicLong orderId = new AtomicLong(200L);
        when(orders.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) order.setId(orderId.getAndIncrement());
            return order;
        });
        when(items.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("[L1-IM-01] ten valid XLSX rows complete with ten accepted")
    void importsTenValidRows() throws Exception {
        ImportBatchResponse response = service.importExcel(workbook(validRows(10)), deliveryDate, false, 9L);
        assertAll(() -> assertEquals("COMPLETED", response.getStatus()),
                () -> assertEquals(10, response.getAcceptedRows()),
                () -> assertEquals(0, response.getRejectedRows()),
                () -> assertNotNull(response.getBatchId()));
    }

    @Test
    @DisplayName("[L1-IM-02] confirmReplace deactivates the old active batch before creating the new batch")
    void replaceDeactivatesOldBatch() throws Exception {
        ImportBatch old = ImportBatch.builder().id(77L).deliveryDate(deliveryDate).isActive(true).build();
        lenient().when(batches.findAllActiveByDate(deliveryDate)).thenReturn(List.of(old));
        ImportBatchResponse response = service.importExcel(workbook(List.of()), deliveryDate, true, 9L);
        assertAll(() -> assertFalse(old.getIsActive(), "confirmReplace must deactivate the prior active batch"),
                () -> verify(batches).save(old),
                () -> assertEquals(Boolean.TRUE, response.getIsActive()));
    }

    @Test
    @DisplayName("[L1-IM-03] eight valid and two unknown-store rows produce exact accepted and rejected counts")
    void importsMixedEightAndTwo() throws Exception {
        List<String[]> rows = new ArrayList<>(validRows(8));
        rows.add(row("BAD-1", "UNKNOWN-1"));
        rows.add(row("BAD-2", "UNKNOWN-2"));
        ImportBatchResponse response = service.importExcel(workbook(rows), deliveryDate, false, 9L);
        ArgumentCaptor<List<ImportError>> captor = errorListCaptor();
        verify(errors).saveAll(captor.capture());
        assertAll(() -> assertEquals(8, response.getAcceptedRows()),
                () -> assertEquals(2, response.getRejectedRows()),
                () -> assertEquals(2, captor.getValue().size()));
    }

    @Test
    @DisplayName("[L1-IM-04] duplicate order store and product accumulates quantity into one item")
    void accumulatesDuplicateProductQuantity() throws Exception {
        List<String[]> rows = List.of(
                new String[]{"ORD-001", "ST-001", "SKU-A", "5"},
                new String[]{"ORD-001", "ST-001", "SKU-A", "5"});
        ImportBatchResponse response = service.importExcel(workbook(rows), deliveryDate, false, 9L);
        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(items, times(2)).save(captor.capture());
        assertAll(() -> assertEquals(2, response.getAcceptedRows()),
                () -> verify(orders, times(1)).save(any(Order.class)),
                () -> assertEquals(10, captor.getValue().getQuantity()));
    }

    @Test
    @DisplayName("[L1-IM-05] csv input is rejected as EXCEL_PARSE_ERROR before batch creation")
    void rejectsNonXlsx() {
        MultipartFile csv = new MockMultipartFile("file", "orders.csv", "text/csv", "a,b".getBytes());
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.importExcel(csv, deliveryDate, false, 9L));
        assertAll(() -> assertEquals(ErrorCode.EXCEL_PARSE_ERROR, error.getErrorCode()),
                () -> assertEquals(HttpStatus.BAD_REQUEST, error.getHttpStatus()),
                () -> verify(batches, never()).save(any()));
    }

    @Test
    @DisplayName("[L1-IM-06] header-only workbook completes an empty batch through processing and completed saves")
    void importsHeaderOnlyWorkbook() throws Exception {
        List<String> statusesAtSave = new ArrayList<>();
        when(batches.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch batch = invocation.getArgument(0);
            statusesAtSave.add(batch.getStatus());
            if (batch.getId() == null) batch.setId(100L);
            return batch;
        });
        ImportBatchResponse response = service.importExcel(workbook(List.of()), deliveryDate, false, 9L);
        assertAll(() -> assertEquals(List.of("PROCESSING", "COMPLETED"), statusesAtSave),
                () -> assertEquals(0, response.getTotalRows()),
                () -> assertEquals(0, response.getAcceptedRows()),
                () -> assertEquals(0, response.getRejectedRows()));
    }

    @Test
    @DisplayName("[L1-IM-07] null and empty files are rejected before batch creation")
    void rejectsNullAndEmptyFiles() {
        MultipartFile empty = new MockMultipartFile("file", "empty.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        BusinessException nullError = assertThrows(BusinessException.class,
                () -> service.importExcel(null, deliveryDate, false, 9L));
        BusinessException emptyError = assertThrows(BusinessException.class,
                () -> service.importExcel(empty, deliveryDate, false, 9L));
        assertAll(() -> assertEquals(ErrorCode.EXCEL_PARSE_ERROR, nullError.getErrorCode()),
                () -> assertEquals(ErrorCode.EXCEL_PARSE_ERROR, emptyError.getErrorCode()),
                () -> assertEquals(HttpStatus.BAD_REQUEST, emptyError.getHttpStatus()),
                () -> verify(batches, never()).save(any()));
    }

    @Test
    @DisplayName("[L1-IM-08] corrupt xlsx bytes are translated to EXCEL_PARSE_ERROR")
    void rejectsCorruptWorkbook() {
        MultipartFile corrupt = new MockMultipartFile("file", "corrupt.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3, 4});
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.importExcel(corrupt, deliveryDate, false, 9L));
        assertAll(() -> assertEquals(ErrorCode.EXCEL_PARSE_ERROR, error.getErrorCode()),
                () -> assertEquals(HttpStatus.BAD_REQUEST, error.getHttpStatus()),
                () -> verify(batches, never()).save(any()));
    }

    @Test
    @DisplayName("[L1-IM-09] unknown store creates a storeCode not-found import error")
    void rejectsUnknownStoreWithFieldEvidence() throws Exception {
        ImportBatchResponse response = service.importExcel(workbook(Collections.singletonList(row("BAD-1", "UNKNOWN_STORE"))), deliveryDate, false, 9L);
        ArgumentCaptor<ImportError> captor = ArgumentCaptor.forClass(ImportError.class);
        verify(errors).save(captor.capture());
        assertAll(() -> assertEquals(0, response.getAcceptedRows()),
                () -> assertEquals(1, response.getRejectedRows()),
                () -> assertEquals("STORE_NOT_FOUND", captor.getValue().getErrorCode()),
                () -> assertEquals("storeCode", captor.getValue().getFieldName()),
                () -> assertTrue(captor.getValue().getErrorReason().toLowerCase().contains("not found") || captor.getValue().getErrorReason().toLowerCase().contains("không tồn tại")));
    }

    @Test
    @DisplayName("[L1-IM-16] all valid rows persist batch status COMPLETED with zero rejects")
    void allValidRowsSetCompletedStatus() throws Exception {
        ImportBatchResponse response = service.importExcel(workbook(validRows(10)), deliveryDate, false, 9L);
        ArgumentCaptor<ImportBatch> captor = ArgumentCaptor.forClass(ImportBatch.class);
        verify(batches, atLeastOnce()).save(captor.capture());
        ImportBatch last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertAll(() -> assertEquals("COMPLETED", last.getStatus()),
                () -> assertEquals(10, last.getAcceptedRows()),
                () -> assertEquals(0, last.getRejectedRows()));
    }

    @Test
    @DisplayName("[L1-IM-17] five valid and five invalid rows set a partial-success batch status")
    void mixedRowsSetPartialStatus() throws Exception {
        List<String[]> rows = new ArrayList<>(validRows(5));
        for (int i = 0; i < 5; i++) rows.add(row("BAD-" + i, "UNKNOWN-" + i));
        ImportBatchResponse response = service.importExcel(workbook(rows), deliveryDate, false, 9L);
        assertAll(() -> assertEquals(5, response.getAcceptedRows()),
                () -> assertEquals(5, response.getRejectedRows()),
                () -> assertEquals("PARTIAL", response.getStatus(), "mixed results need an observable partial-success status"));
    }

    @Test
    @DisplayName("[L1-IM-18] ten invalid stores create a batch with zero accepted and no orders")
    void allInvalidRowsCreateNoOrders() throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < 10; i++) rows.add(row("BAD-" + i, "UNKNOWN-" + i));
        ImportBatchResponse response = service.importExcel(workbook(rows), deliveryDate, false, 9L);
        ArgumentCaptor<List<ImportError>> captor = errorListCaptor();
        verify(errors).saveAll(captor.capture());
        assertAll(() -> assertEquals(0, response.getAcceptedRows()),
                () -> assertEquals(10, response.getRejectedRows()),
                () -> assertEquals(10, captor.getValue().size()),
                () -> verify(orders, never()).save(any()));
    }

    private List<String[]> validRows(int count) {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) rows.add(row("ORD-" + i, "ST-001"));
        return rows;
    }

    private String[] row(String orderRef, String storeCode) {
        return new String[]{orderRef, storeCode, "SKU-A", "1"};
    }

    private MultipartFile workbook(List<String[]> data) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("orders");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("orderRef");
            header.createCell(1).setCellValue("storeCode");
            header.createCell(2).setCellValue("sku");
            header.createCell(3).setCellValue("quantity");
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                for (int column = 0; column < data.get(i).length; column++) {
                    row.createCell(column).setCellValue(data.get(i)[column]);
                }
            }
            workbook.write(output);
            return new MockMultipartFile("file", "orders.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<ImportError>> errorListCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }

    @Test
    @DisplayName("[L1-IMP-09] importExcel rejects null file")
    void importExcelRejectsNullFile() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.importExcel(null, LocalDate.now(), false, 1L));
        assertEquals(ErrorCode.EXCEL_PARSE_ERROR, e.getErrorCode());
    }

    @Test
    @DisplayName("[L1-IMP-10] importExcel rejects non-excel file extension")
    void importExcelRejectsNonExcelExtension() {
        MockMultipartFile txtFile = new MockMultipartFile("file", "data.txt", "text/plain", "hello".getBytes());
        BusinessException e = assertThrows(BusinessException.class, () -> service.importExcel(txtFile, LocalDate.now(), false, 1L));
        assertEquals(ErrorCode.EXCEL_PARSE_ERROR, e.getErrorCode());
    }

    @Test
    @DisplayName("[L1-IMP-11] importExcel rejects empty file")
    void importExcelRejectsEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "orders.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        BusinessException e = assertThrows(BusinessException.class, () -> service.importExcel(emptyFile, LocalDate.now(), false, 1L));
        assertEquals(ErrorCode.EXCEL_PARSE_ERROR, e.getErrorCode());
    }

    // ── Additional Unit Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("[L1-IMP-12] getBatches with date returns batches page")
    void getBatchesWithDate() {
        ImportBatch b = ImportBatch.builder().id(100L).deliveryDate(deliveryDate).status("SUCCESS").build();
        org.springframework.data.domain.Page<ImportBatch> page = new org.springframework.data.domain.PageImpl<>(List.of(b));
        when(batches.findByDeliveryDate(eq(deliveryDate), any())).thenReturn(page);
        when(orders.countByBatchId(100L)).thenReturn(5L);

        var resp = service.getBatches(deliveryDate, org.springframework.data.domain.PageRequest.of(0, 10));
        assertNotNull(resp);
        assertEquals(1, resp.getData().size());
        assertEquals(100L, resp.getData().get(0).getBatchId());
    }

    @Test
    @DisplayName("[L1-IMP-13] getBatchById returns batch detail or 404")
    void getBatchByIdTests() {
        ImportBatch b = ImportBatch.builder().id(100L).deliveryDate(deliveryDate).status("SUCCESS").build();
        when(batches.findById(100L)).thenReturn(Optional.of(b));
        when(batches.findById(999L)).thenReturn(Optional.empty());
        when(orders.countByBatchId(100L)).thenReturn(5L);

        var resp = service.getBatchById(100L);
        assertNotNull(resp);
        assertEquals(100L, resp.getBatchId());

        assertThrows(BusinessException.class, () -> service.getBatchById(999L));
    }

    @Test
    @DisplayName("[L1-IMP-14] getBatchErrors returns error list or 404")
    void getBatchErrorsTests() {
        when(batches.existsById(100L)).thenReturn(true);
        when(batches.existsById(999L)).thenReturn(false);

        ImportError err = ImportError.builder().id(1L).rowNumber(2).errorCode("SKU_NOT_FOUND").errorReason("Unknown sku").build();
        org.springframework.data.domain.Page<ImportError> page = new org.springframework.data.domain.PageImpl<>(List.of(err));
        when(errors.findByImportBatchIdAndErrorCode(eq(100L), any(), any())).thenReturn(page);

        var resp = service.getBatchErrors(100L, "SKU_NOT_FOUND", org.springframework.data.domain.PageRequest.of(0, 10));
        assertNotNull(resp);
        assertEquals(1, resp.getData().size());

        assertThrows(BusinessException.class, () -> service.getBatchErrors(999L, null, org.springframework.data.domain.PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("[L1-IMP-15] exportBatchErrors exports excel byte array")
    void exportBatchErrorsTests() {
        when(batches.existsById(100L)).thenReturn(true);
        when(batches.existsById(999L)).thenReturn(false);

        ImportError err = ImportError.builder().id(1L).rowNumber(2).errorCode("SKU_NOT_FOUND").fieldName("SKU").rawData("SKU-999").errorReason("Unknown sku").build();
        when(errors.findByImportBatchIdOrderByRowNumberAsc(100L)).thenReturn(List.of(err));

        byte[] excelBytes = service.exportBatchErrors(100L);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        assertThrows(BusinessException.class, () -> service.exportBatchErrors(999L));
    }

    @Test
    @DisplayName("[L1-IMP-16] getImportedOrders returns orders for batch")
    void getImportedOrdersTests() {
        when(batches.existsById(100L)).thenReturn(true);
        OrderItem item = OrderItem.builder().sku("SKU-A").product(product).quantity(5).lineWeightKg(BigDecimal.valueOf(5)).lineVolumeM3(BigDecimal.valueOf(0.5)).build();
        Order ord = Order.builder().id(200L).orderRef("ORD-200").store(store).deliveryDate(deliveryDate).status("IMPORTED").items(List.of(item)).build();
        when(orders.findByImportBatchId(100L)).thenReturn(List.of(ord));

        var ordersList = service.getImportedOrders(100L);
        assertNotNull(ordersList);
        assertEquals(1, ordersList.size());
        assertEquals("ORD-200", ordersList.get(0).getOrderRef());
    }
}

