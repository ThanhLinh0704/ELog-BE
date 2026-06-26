package com.elog.service.impl;

import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.ImportBatchResponse;
import com.elog.dto.response.ImportErrorResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    private final ImportBatchRepository batchRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ImportErrorRepository errorRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    // ── POST /api/imports ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public ImportBatchResponse importExcel(MultipartFile file, LocalDate deliveryDate,
                                            boolean confirmReplace, Long uploadedBy) {
        // Step 1: Validate file format (BEFORE transaction — nothing to rollback)
        validateFile(file);

        // Step 2: Check existing active batch for this delivery date
        Optional<ImportBatch> existingBatch = batchRepository.findActiveByDate(deliveryDate);
        if (existingBatch.isPresent()) {
            if (!confirmReplace) {
                throw new DuplicateBatchException(existingBatch.get().getId(), deliveryDate);
            }
            // Deactivate old batch (soft replace)
            ImportBatch oldBatch = existingBatch.get();
            oldBatch.setIsActive(false);
            batchRepository.save(oldBatch);
        }

        // Step 3: Create new batch
        ImportBatch batch;
        try {
            batch = ImportBatch.builder()
                    .deliveryDate(deliveryDate)
                    .fileName(file.getOriginalFilename())
                    .uploadedBy(uploadedBy)
                    .status("PROCESSING")
                    .build();
            batch = batchRepository.save(batch);
            batchRepository.flush(); // force unique constraint check
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request created a batch for this date concurrently
            log.warn("Concurrent batch creation for date {}: {}", deliveryDate, e.getMessage());
            throw new BusinessException(ErrorCode.EXCEL_PARSE_ERROR,
                    "Đã có dữ liệu nhập cho ngày " + deliveryDate + ". Vui lòng thử lại.",
                    HttpStatus.CONFLICT);
        }

        // Step 4: Parse Excel rows
        List<RowData> rows = parseExcelFile(file);

        int totalRows = rows.size();
        int acceptedRows = 0;
        int rejectedRows = 0;

        // Cache for stores and products to avoid repeated DB lookups
        Map<String, Store> storeCache = new HashMap<>();
        Map<String, Product> productCache = new HashMap<>();
        // Cache for orders: key = orderRef + "|" + storeId
        Map<String, Order> orderCache = new HashMap<>();

        for (RowData row : rows) {
            try {
                processRow(row, batch, deliveryDate, storeCache, productCache, orderCache);
                acceptedRows++;
            } catch (RowRejectedException ex) {
                rejectedRows++;
                ImportError error = ImportError.builder()
                        .importBatch(batch)
                        .rowNumber(row.rowNumber)
                        .rawData(row.toRawString())
                        .errorReason(ex.getMessage())
                        .build();
                errorRepository.save(error);
            }
        }

        // Step 5: Update batch summary
        batch.setTotalRows(totalRows);
        batch.setAcceptedRows(acceptedRows);
        batch.setRejectedRows(rejectedRows);
        batch.setStatus("COMPLETED");
        batchRepository.save(batch);

        long ordersCreated = orderRepository.countByBatchId(batch.getId());

        return toResponse(batch, ordersCreated);
    }

    // ── GET /api/imports ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ImportBatchResponse>> getBatches(LocalDate deliveryDate, Pageable pageable) {
        Page<ImportBatch> page;
        if (deliveryDate != null) {
            page = batchRepository.findByDeliveryDate(deliveryDate, pageable);
        } else {
            page = batchRepository.findAllBatches(pageable);
        }

        List<ImportBatchResponse> items = page.getContent().stream()
                .map(b -> toResponse(b, orderRepository.countByBatchId(b.getId())))
                .toList();

        return ApiResponse.<List<ImportBatchResponse>>builder()
                .success(true)
                .data(items)
                .pagination(ApiResponse.PaginationInfo.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    // ── GET /api/imports/{batchId} ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ImportBatchResponse getBatchById(Long batchId) {
        ImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Import batch not found: " + batchId, HttpStatus.NOT_FOUND));
        long ordersCreated = orderRepository.countByBatchId(batchId);
        return toResponse(batch, ordersCreated);
    }

    // ── GET /api/imports/{batchId}/errors ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ImportErrorResponse> getBatchErrors(Long batchId) {
        // Verify batch exists
        if (!batchRepository.existsById(batchId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Import batch not found: " + batchId, HttpStatus.NOT_FOUND);
        }

        return errorRepository.findByImportBatchIdOrderByRowNumberAsc(batchId).stream()
                .map(e -> ImportErrorResponse.builder()
                        .rowNumber(e.getRowNumber())
                        .rawData(e.getRawData())
                        .errorReason(e.getErrorReason())
                        .build())
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EXCEL_PARSE_ERROR,
                    "File không được để trống", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException(ErrorCode.EXCEL_PARSE_ERROR,
                    "Chỉ hỗ trợ file định dạng .xlsx", HttpStatus.BAD_REQUEST);
        }
    }

    private List<RowData> parseExcelFile(MultipartFile file) {
        List<RowData> rows = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException(ErrorCode.EXCEL_PARSE_ERROR,
                        "File Excel không có sheet nào", HttpStatus.BAD_REQUEST);
            }

            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                RowData data = new RowData();
                data.rowNumber = i + 1; // 1-indexed, counting header
                data.orderRef = getCellStringValue(row.getCell(0));
                data.storeCode = getCellStringValue(row.getCell(1));
                data.sku = getCellStringValue(row.getCell(2));
                data.quantityRaw = getCellStringValue(row.getCell(3));
                rows.add(data);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing Excel file: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXCEL_PARSE_ERROR,
                    "File Excel bị hỏng hoặc không đọc được", HttpStatus.BAD_REQUEST);
        }
        return rows;
    }

    private void processRow(RowData row, ImportBatch batch, LocalDate deliveryDate,
                            Map<String, Store> storeCache, Map<String, Product> productCache,
                            Map<String, Order> orderCache) {

        // 4a: Validate quantity
        int quantity;
        try {
            double dVal = Double.parseDouble(row.quantityRaw.trim());
            if (dVal != Math.floor(dVal) || dVal <= 0) {
                throw new NumberFormatException();
            }
            quantity = (int) dVal;
        } catch (NumberFormatException | NullPointerException e) {
            throw new RowRejectedException("Số lượng phải là số nguyên dương (giá trị: '" + row.quantityRaw + "')");
        }

        // 4b: Lookup store (cache cả null để tránh N+1 queries)
        String storeCode = row.storeCode != null ? row.storeCode.trim() : "";
        if (storeCode.isEmpty()) {
            throw new RowRejectedException("Mã cửa hàng không được để trống");
        }
        if (!storeCache.containsKey(storeCode)) {
            storeCache.put(storeCode, storeRepository.findByCode(storeCode).orElse(null));
        }
        Store store = storeCache.get(storeCode);
        if (store == null) {
            throw new RowRejectedException("Mã cửa hàng '" + storeCode + "' không tồn tại trong hệ thống");
        }

        // 4c: Lookup product by SKU
        String sku = row.sku != null ? row.sku.trim() : "";
        if (sku.isEmpty()) {
            throw new RowRejectedException("SKU không được để trống");
        }
        if (!productCache.containsKey(sku)) {
            productCache.put(sku, productRepository.findBySku(sku).orElse(null));
        }
        Product product = productCache.get(sku);
        if (product == null) {
            throw new RowRejectedException("SKU '" + sku + "' chưa có trong danh mục sản phẩm");
        }
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new RowRejectedException("SKU '" + sku + "' đã ngừng kinh doanh");
        }

        // 4d: Determine order_ref
        String orderRef = (row.orderRef != null && !row.orderRef.trim().isEmpty())
                ? row.orderRef.trim()
                : "AUTO-" + batch.getId() + "-" + row.rowNumber;

        // 4e: Find or create Order
        String orderKey = orderRef + "|" + store.getId();
        Order order = orderCache.computeIfAbsent(orderKey, k -> {
            Optional<Order> existing = orderRepository.findByBatchAndOrderRefAndStore(
                    batch.getId(), orderRef, store.getId());
            return existing.orElseGet(() -> {
                Order newOrder = Order.builder()
                        .importBatch(batch)
                        .orderRef(orderRef)
                        .store(store)
                        .deliveryDate(deliveryDate)
                        .build();
                return orderRepository.save(newOrder);
            });
        });

        // 4f: Create OrderItem with snapshot
        BigDecimal unitWeight = product.getWeightKg();
        BigDecimal unitVolume = product.getVolumeM3();
        BigDecimal qty = BigDecimal.valueOf(quantity);

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .sku(sku)
                .quantity(quantity)
                .unitWeightKg(unitWeight)
                .unitVolumeM3(unitVolume)
                .lineWeightKg(unitWeight.multiply(qty).setScale(3, RoundingMode.HALF_UP))
                .lineVolumeM3(unitVolume.multiply(qty).setScale(6, RoundingMode.HALF_UP))
                .build();
        orderItemRepository.save(item);
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < 4; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellStringValue(cell);
                if (val != null && !val.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    double d = cell.getNumericCellValue();
                    if (d == Math.floor(d) && !Double.isInfinite(d)) {
                        yield String.valueOf((long) d);
                    }
                    yield String.valueOf(d);
                }
            }
            default -> "";
        };
    }

    private ImportBatchResponse toResponse(ImportBatch batch, long ordersCreated) {
        return ImportBatchResponse.builder()
                .batchId(batch.getId())
                .deliveryDate(batch.getDeliveryDate())
                .fileName(batch.getFileName())
                .totalRows(batch.getTotalRows())
                .acceptedRows(batch.getAcceptedRows())
                .rejectedRows(batch.getRejectedRows())
                .ordersCreated(ordersCreated)
                .status(batch.getStatus())
                .isActive(batch.getIsActive())
                .createdAt(batch.getCreatedAt())
                .build();
    }

    // ── Inner exception for per-row rejection ─────────────────────────────────

    private static class RowRejectedException extends RuntimeException {
        RowRejectedException(String message) {
            super(message);
        }
    }

    // ── Inner class for parsed row data ───────────────────────────────────────

    private static class RowData {
        int rowNumber;
        String orderRef;
        String storeCode;
        String sku;
        String quantityRaw;

        String toRawString() {
            return String.join(",", nvl(orderRef), nvl(storeCode), nvl(sku), nvl(quantityRaw));
        }

        private String nvl(String s) {
            return s != null ? s : "";
        }
    }

    // ── Custom exception for duplicate batch (HTTP 409) ───────────────────────

    public static class DuplicateBatchException extends RuntimeException {
        private final Long existingBatchId;
        private final LocalDate deliveryDate;

        public DuplicateBatchException(Long existingBatchId, LocalDate deliveryDate) {
            super("Đã có dữ liệu nhập cho ngày " + deliveryDate);
            this.existingBatchId = existingBatchId;
            this.deliveryDate = deliveryDate;
        }

        public Long getExistingBatchId() { return existingBatchId; }
        public LocalDate getDeliveryDate() { return deliveryDate; }
    }
}
