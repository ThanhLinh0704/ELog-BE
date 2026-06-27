package com.elog.service;

import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.ImportBatchResponse;
import com.elog.dto.response.ImportErrorResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.repository.*;
import com.elog.service.impl.ImportServiceImpl;
import com.elog.service.impl.ImportServiceImpl.DuplicateBatchException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportServiceImplTest {

    @Mock
    private ImportBatchRepository batchRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ImportErrorRepository errorRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ImportServiceImpl importService;

    private Store storeBT001;
    private Product productActive;
    private Product productInactive;

    @BeforeEach
    void setUp() {
        storeBT001 = Store.builder()
                .id(1L)
                .code("ST-BT-001")
                .name("Store Binh Thanh")
                .isActive(true)
                .build();

        productActive = Product.builder()
                .id(10L)
                .sku("REF-SAM-300")
                .productName("Samsung Refrigerator")
                .weightKg(BigDecimal.valueOf(65.0))
                .volumeM3(BigDecimal.valueOf(0.714))
                .isActive(true)
                .build();

        productInactive = Product.builder()
                .id(11L)
                .sku("ACC-HDMI-2M")
                .productName("HDMI Cable 2M")
                .weightKg(BigDecimal.valueOf(0.2))
                .volumeM3(BigDecimal.valueOf(0.001))
                .isActive(false)
                .build();
    }

    private MultipartFile createMockExcelFile(String fileName, List<String[]> rowsData) throws IOException {
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

    // ── L1-IBS-01 ──────────────────────────────────────────
    @Test
    void createBatch_noActiveBatch_success() throws IOException {
        LocalDate date = LocalDate.now();
        MultipartFile file = createMockExcelFile("import.xlsx", Collections.emptyList());

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getBatchId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(batchRepository, times(2)).save(any(ImportBatch.class)); // 1 for creation, 1 for summary update
        verify(batchRepository, never()).save(argThat(b -> !b.getIsActive() && b.getId() != null)); // No deactivation
    }

    // ── L1-IBS-02 ──────────────────────────────────────────
    @Test
    void createBatch_activeBatchExists_confirmReplaceFalse_throwsException() throws IOException {
        LocalDate date = LocalDate.now();
        MultipartFile file = createMockExcelFile("import.xlsx", Collections.emptyList());
        ImportBatch existing = ImportBatch.builder().id(8L).deliveryDate(date).isActive(true).build();

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> importService.importExcel(file, date, false, 1L))
                .isInstanceOf(DuplicateBatchException.class)
                .hasMessageContaining("Đã có dữ liệu nhập cho ngày");
        
        verify(batchRepository, never()).save(any(ImportBatch.class));
    }

    // ── L1-IBS-03 ──────────────────────────────────────────
    @Test
    void createBatch_activeBatchExists_confirmReplaceTrue_deactivatesOld() throws IOException {
        LocalDate date = LocalDate.now();
        MultipartFile file = createMockExcelFile("import.xlsx", Collections.emptyList());
        ImportBatch existing = ImportBatch.builder().id(8L).deliveryDate(date).isActive(true).build();

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.of(existing));
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            if (b.getId() == null) b.setId(9L);
            return b;
        });

        ImportBatchResponse response = importService.importExcel(file, date, true, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getBatchId()).isEqualTo(9L);
        assertThat(existing.getIsActive()).isFalse(); // verify old deactivated
        verify(batchRepository).save(existing);
    }

    // ── L1-EIS-01 ──────────────────────────────────────────
    @Test
    void parseRow_validRow_createsOrderItemWithSnapshot() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "REF-SAM-300", "2"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });
        
        when(storeRepository.findByCode("ST-BT-001")).thenReturn(Optional.of(storeBT001));
        when(productRepository.findBySku("REF-SAM-300")).thenReturn(Optional.of(productActive));
        
        when(orderRepository.findByBatchAndOrderRefAndStore(anyLong(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getAcceptedRows()).isEqualTo(1);
        assertThat(response.getRejectedRows()).isEqualTo(0);

        verify(orderItemRepository).save(argThat(item -> 
            item.getSku().equals("REF-SAM-300") &&
            item.getQuantity() == 2 &&
            item.getUnitWeightKg().compareTo(BigDecimal.valueOf(65.0)) == 0 &&
            item.getUnitVolumeM3().compareTo(BigDecimal.valueOf(0.714)) == 0 &&
            item.getLineWeightKg().compareTo(BigDecimal.valueOf(130.0)) == 0 &&
            item.getLineVolumeM3().compareTo(BigDecimal.valueOf(1.428)) == 0
        ));
    }

    // ── L1-EIS-02 ──────────────────────────────────────────
    @Test
    void parseRow_skuNotExist_rejectsRow() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "ACC-HDMI-2M", "2"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(storeRepository.findByCode("ST-BT-001")).thenReturn(Optional.of(storeBT001));
        when(productRepository.findBySku("ACC-HDMI-2M")).thenReturn(Optional.empty());

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getAcceptedRows()).isEqualTo(0);
        assertThat(response.getRejectedRows()).isEqualTo(1);

        verify(errorRepository).save(argThat(err -> 
            err.getRowNumber() == 2 && 
            err.getErrorReason().contains("SKU 'ACC-HDMI-2M' chưa có trong danh mục sản phẩm")
        ));
        verify(orderItemRepository, never()).save(any());
    }

    // ── L1-EIS-03 ──────────────────────────────────────────
    @Test
    void parseRow_storeCodeNotExist_rejectsRow() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-HD-099", "REF-SAM-300", "2"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(storeRepository.findByCode("ST-HD-099")).thenReturn(Optional.empty());

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getAcceptedRows()).isEqualTo(0);
        assertThat(response.getRejectedRows()).isEqualTo(1);

        verify(errorRepository).save(argThat(err -> 
            err.getRowNumber() == 2 && 
            err.getErrorReason().contains("Mã cửa hàng 'ST-HD-099' không tồn tại")
        ));
        verify(orderItemRepository, never()).save(any());
    }

    // ── L1-EIS-04 ──────────────────────────────────────────
    @Test
    void parseRow_quantityZero_rejectsRow() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "REF-SAM-300", "0"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getAcceptedRows()).isEqualTo(0);
        assertThat(response.getRejectedRows()).isEqualTo(1);

        verify(errorRepository).save(argThat(err -> 
            err.getRowNumber() == 2 && 
            err.getErrorReason().contains("Số lượng phải là số nguyên dương")
        ));
        verify(orderItemRepository, never()).save(any());
    }

    // ── L1-EIS-05 ──────────────────────────────────────────
    @Test
    void parseRow_quantityOne_acceptsRow() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "REF-SAM-300", "1"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(storeRepository.findByCode("ST-BT-001")).thenReturn(Optional.of(storeBT001));
        when(productRepository.findBySku("REF-SAM-300")).thenReturn(Optional.of(productActive));
        when(orderRepository.findByBatchAndOrderRefAndStore(anyLong(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getAcceptedRows()).isEqualTo(1);
        assertThat(response.getRejectedRows()).isEqualTo(0);
        verify(orderItemRepository).save(argThat(item -> item.getQuantity() == 1));
    }

    // ── L1-EIS-06 ──────────────────────────────────────────
    @Test
    void parseRow_skuInactive_rejectsRow() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "ACC-HDMI-2M", "2"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(storeRepository.findByCode("ST-BT-001")).thenReturn(Optional.of(storeBT001));
        when(productRepository.findBySku("ACC-HDMI-2M")).thenReturn(Optional.of(productInactive));

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getAcceptedRows()).isEqualTo(0);
        assertThat(response.getRejectedRows()).isEqualTo(1);

        verify(errorRepository).save(argThat(err -> 
            err.getRowNumber() == 2 && 
            err.getErrorReason().contains("đã ngừng kinh doanh")
        ));
        verify(orderItemRepository, never()).save(any());
    }

    // ── L1-EIS-07 ──────────────────────────────────────────
    @Test
    void groupIntoOrders_sameOrderRefAndStore_groupsIntoOneOrder() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "REF-SAM-300", "2"});
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "REF-SAM-300", "3"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(storeRepository.findByCode("ST-BT-001")).thenReturn(Optional.of(storeBT001));
        when(productRepository.findBySku("REF-SAM-300")).thenReturn(Optional.of(productActive));

        Order order = Order.builder().id(100L).orderRef("DH160325-01").store(storeBT001).build();
        when(orderRepository.findByBatchAndOrderRefAndStore(1L, "DH160325-01", 1L))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        importService.importExcel(file, date, false, 1L);

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
    }

    // ── L1-EIS-08 ──────────────────────────────────────────
    @Test
    void groupIntoOrders_differentOrderRef_createsSeparateOrders() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "REF-SAM-300", "2"});
        rowsData.add(new String[]{"DH160325-02", "ST-BT-001", "REF-SAM-300", "3"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(storeRepository.findByCode("ST-BT-001")).thenReturn(Optional.of(storeBT001));
        when(productRepository.findBySku("REF-SAM-300")).thenReturn(Optional.of(productActive));

        when(orderRepository.findByBatchAndOrderRefAndStore(eq(1L), anyString(), eq(1L)))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(new Random().nextLong());
            return o;
        });

        importService.importExcel(file, date, false, 1L);

        verify(orderRepository, times(2)).save(any(Order.class));
    }

    // ── L1-EIS-09 ──────────────────────────────────────────
    @Test
    void parseRow_missingOrderRef_generatesAutoOrderRef() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{null, "ST-BT-001", "REF-SAM-300", "2"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(storeRepository.findByCode("ST-BT-001")).thenReturn(Optional.of(storeBT001));
        when(productRepository.findBySku("REF-SAM-300")).thenReturn(Optional.of(productActive));
        when(orderRepository.findByBatchAndOrderRefAndStore(eq(1L), startsWith("AUTO-1-"), eq(1L)))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });

        importService.importExcel(file, date, false, 1L);

        verify(orderRepository).save(argThat(order -> order.getOrderRef().equals("AUTO-1-2")));
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    // ── Additional coverage tests ───────────────────────────

    @Test
    void getBatches_withDeliveryDate_success() {
        LocalDate date = LocalDate.now();
        Pageable pageable = mock(Pageable.class);
        Page<ImportBatch> page = mock(Page.class);
        ImportBatch batch = ImportBatch.builder().id(1L).deliveryDate(date).isActive(true).build();

        when(page.getContent()).thenReturn(List.of(batch));
        when(page.getNumber()).thenReturn(0);
        when(page.getSize()).thenReturn(10);
        when(page.getTotalElements()).thenReturn(1L);
        when(page.getTotalPages()).thenReturn(1);
        when(batchRepository.findByDeliveryDate(date, pageable)).thenReturn(page);

        ApiResponse<List<ImportBatchResponse>> response = importService.getBatches(date, pageable);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        verify(batchRepository).findByDeliveryDate(date, pageable);
    }

    @Test
    void getBatches_withoutDeliveryDate_success() {
        Pageable pageable = mock(Pageable.class);
        Page<ImportBatch> page = mock(Page.class);
        ImportBatch batch = ImportBatch.builder().id(1L).deliveryDate(LocalDate.now()).isActive(true).build();

        when(page.getContent()).thenReturn(List.of(batch));
        when(page.getNumber()).thenReturn(0);
        when(page.getSize()).thenReturn(10);
        when(page.getTotalElements()).thenReturn(1L);
        when(page.getTotalPages()).thenReturn(1);
        when(batchRepository.findAllBatches(pageable)).thenReturn(page);

        ApiResponse<List<ImportBatchResponse>> response = importService.getBatches(null, pageable);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        verify(batchRepository).findAllBatches(pageable);
    }

    @Test
    void getBatchById_success() {
        ImportBatch batch = ImportBatch.builder().id(1L).isActive(true).build();
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(orderRepository.countByBatchId(1L)).thenReturn(5L);

        ImportBatchResponse response = importService.getBatchById(1L);

        assertThat(response.getBatchId()).isEqualTo(1L);
        assertThat(response.getOrdersCreated()).isEqualTo(5L);
    }

    @Test
    void getBatchById_notFound_throwsException() {
        when(batchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importService.getBatchById(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Import batch not found: 99");
    }

    @Test
    void getBatchErrors_success() {
        when(batchRepository.existsById(1L)).thenReturn(true);
        ImportError error = ImportError.builder().rowNumber(5).rawData("raw").errorReason("reason").build();
        when(errorRepository.findByImportBatchIdOrderByRowNumberAsc(1L)).thenReturn(List.of(error));

        List<ImportErrorResponse> response = importService.getBatchErrors(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRowNumber()).isEqualTo(5);
    }

    @Test
    void getBatchErrors_notFound_throwsException() {
        when(batchRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> importService.getBatchErrors(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Import batch not found: 99");
    }

    @Test
    void importExcel_nullFile_throwsException() {
        assertThatThrownBy(() -> importService.importExcel(null, LocalDate.now(), false, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("File không được để trống");
    }

    @Test
    void importExcel_emptyFile_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        assertThatThrownBy(() -> importService.importExcel(file, LocalDate.now(), false, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("File không được để trống");
    }

    @Test
    void importExcel_invalidExtension_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
        assertThatThrownBy(() -> importService.importExcel(file, LocalDate.now(), false, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chỉ hỗ trợ file định dạng .xlsx");
    }

    @Test
    void processRow_emptyStoreCode_rejectsRow() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "", "REF-SAM-300", "2"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getAcceptedRows()).isEqualTo(0);
        assertThat(response.getRejectedRows()).isEqualTo(1);
        verify(errorRepository).save(argThat(err -> err.getErrorReason().contains("Mã cửa hàng không được để trống")));
    }

    @Test
    void processRow_emptySku_rejectsRow() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "", "2"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(storeRepository.findByCode("ST-BT-001")).thenReturn(Optional.of(storeBT001));

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getAcceptedRows()).isEqualTo(0);
        assertThat(response.getRejectedRows()).isEqualTo(1);
        verify(errorRepository).save(argThat(err -> err.getErrorReason().contains("SKU không được để trống")));
    }

    @Test
    void processRow_invalidQuantityFormat_rejectsRow() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "REF-SAM-300", "abc"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getAcceptedRows()).isEqualTo(0);
        assertThat(response.getRejectedRows()).isEqualTo(1);
        verify(errorRepository).save(argThat(err -> err.getErrorReason().contains("Số lượng phải là số nguyên dương")));
    }

    @Test
    void processRow_negativeQuantity_rejectsRow() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "REF-SAM-300", "-5"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getAcceptedRows()).isEqualTo(0);
        assertThat(response.getRejectedRows()).isEqualTo(1);
        verify(errorRepository).save(argThat(err -> err.getErrorReason().contains("Số lượng phải là số nguyên dương")));
    }

    @Test
    void processRow_decimalQuantity_rejectsRow() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "REF-SAM-300", "2.5"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getAcceptedRows()).isEqualTo(0);
        assertThat(response.getRejectedRows()).isEqualTo(1);
        verify(errorRepository).save(argThat(err -> err.getErrorReason().contains("Số lượng phải là số nguyên dương")));
    }

    @Test
    void parseExcelFile_emptyRow_skipped() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "REF-SAM-300", "2"});
        rowsData.add(new String[]{"", " ", null, ""});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(storeRepository.findByCode("ST-BT-001")).thenReturn(Optional.of(storeBT001));
        when(productRepository.findBySku("REF-SAM-300")).thenReturn(Optional.of(productActive));
        when(orderRepository.findByBatchAndOrderRefAndStore(anyLong(), anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });

        ImportBatchResponse response = importService.importExcel(file, date, false, 1L);

        assertThat(response.getTotalRows()).isEqualTo(1);
        assertThat(response.getAcceptedRows()).isEqualTo(1);
        assertThat(response.getRejectedRows()).isEqualTo(0);
    }

    @Test
    void parseExcelFile_noSheet_throwsException() throws IOException {
        MultipartFile file;
        try (Workbook workbook = new XSSFWorkbook()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            byte[] bytes = bos.toByteArray();
            file = new MockMultipartFile("file", "import.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
        }

        assertThatThrownBy(() -> importService.importExcel(file, LocalDate.now(), false, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("File Excel bị hỏng hoặc không đọc được");
    }

    @Test
    void processRow_orderExistsInDb_reusesOrder() throws IOException {
        LocalDate date = LocalDate.now();
        List<String[]> rowsData = new ArrayList<>();
        rowsData.add(new String[]{"DH160325-01", "ST-BT-001", "REF-SAM-300", "2"});
        MultipartFile file = createMockExcelFile("import.xlsx", rowsData);

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenAnswer(invocation -> {
            ImportBatch b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(storeRepository.findByCode("ST-BT-001")).thenReturn(Optional.of(storeBT001));
        when(productRepository.findBySku("REF-SAM-300")).thenReturn(Optional.of(productActive));

        Order existingOrder = Order.builder().id(100L).orderRef("DH160325-01").store(storeBT001).build();
        when(orderRepository.findByBatchAndOrderRefAndStore(1L, "DH160325-01", 1L))
                .thenReturn(Optional.of(existingOrder));

        importService.importExcel(file, date, false, 1L);

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository).save(argThat(item -> item.getOrder().getId() == 100L));
    }

    @Test
    void createBatch_concurrentCreation_throwsConflictException() throws IOException {
        LocalDate date = LocalDate.now();
        MultipartFile file = createMockExcelFile("import.xlsx", Collections.emptyList());

        when(batchRepository.findActiveByDate(date)).thenReturn(Optional.empty());
        when(batchRepository.save(any(ImportBatch.class))).thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        assertThatThrownBy(() -> importService.importExcel(file, date, false, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Đã có dữ liệu nhập cho ngày")
                .extracting(e -> ((BusinessException) e).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void getCellStringValue_variousCellTypes() throws Exception {
        java.lang.reflect.Method method = ImportServiceImpl.class.getDeclaredMethod("getCellStringValue", Cell.class);
        method.setAccessible(true);

        String valNull = (String) method.invoke(importService, (Cell) null);
        assertThat(valNull).isEmpty();

        Cell cellString = mock(Cell.class);
        when(cellString.getCellType()).thenReturn(CellType.STRING);
        when(cellString.getStringCellValue()).thenReturn("hello");
        String valString = (String) method.invoke(importService, cellString);
        assertThat(valString).isEqualTo("hello");

        Cell cellNumericDouble = mock(Cell.class);
        when(cellNumericDouble.getCellType()).thenReturn(CellType.NUMERIC);
        when(cellNumericDouble.getNumericCellValue()).thenReturn(123.45);
        String valNumericDouble = (String) method.invoke(importService, cellNumericDouble);
        assertThat(valNumericDouble).isEqualTo("123.45");

        Cell cellNumericInt = mock(Cell.class);
        when(cellNumericInt.getCellType()).thenReturn(CellType.NUMERIC);
        when(cellNumericInt.getNumericCellValue()).thenReturn(5.0);
        String valNumericInt = (String) method.invoke(importService, cellNumericInt);
        assertThat(valNumericInt).isEqualTo("5");

        Cell cellBoolean = mock(Cell.class);
        when(cellBoolean.getCellType()).thenReturn(CellType.BOOLEAN);
        when(cellBoolean.getBooleanCellValue()).thenReturn(true);
        String valBoolean = (String) method.invoke(importService, cellBoolean);
        assertThat(valBoolean).isEqualTo("true");

        Cell cellFormulaStr = mock(Cell.class);
        when(cellFormulaStr.getCellType()).thenReturn(CellType.FORMULA);
        when(cellFormulaStr.getStringCellValue()).thenReturn("formula_str");
        String valFormulaStr = (String) method.invoke(importService, cellFormulaStr);
        assertThat(valFormulaStr).isEqualTo("formula_str");

        Cell cellFormulaNumDouble = mock(Cell.class);
        when(cellFormulaNumDouble.getCellType()).thenReturn(CellType.FORMULA);
        when(cellFormulaNumDouble.getStringCellValue()).thenThrow(new IllegalStateException());
        when(cellFormulaNumDouble.getNumericCellValue()).thenReturn(9.75);
        String valFormulaNumDouble = (String) method.invoke(importService, cellFormulaNumDouble);
        assertThat(valFormulaNumDouble).isEqualTo("9.75");

        Cell cellFormulaNumInt = mock(Cell.class);
        when(cellFormulaNumInt.getCellType()).thenReturn(CellType.FORMULA);
        when(cellFormulaNumInt.getStringCellValue()).thenThrow(new IllegalStateException());
        when(cellFormulaNumInt.getNumericCellValue()).thenReturn(10.0);
        String valFormulaNumInt = (String) method.invoke(importService, cellFormulaNumInt);
        assertThat(valFormulaNumInt).isEqualTo("10");

        Cell cellBlank = mock(Cell.class);
        when(cellBlank.getCellType()).thenReturn(CellType.BLANK);
        String valBlank = (String) method.invoke(importService, cellBlank);
        assertThat(valBlank).isEmpty();
    }
}
