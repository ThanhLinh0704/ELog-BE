package com.elog.service.impl;

import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.ImportBatchResponse;
import com.elog.dto.response.ImportErrorResponse;
import com.elog.dto.response.ImportedOrderDetailResponse;
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
    private final UserRepository userRepository;
    private final TripDraftRepository tripDraftRepository;

    // Auto-pipeline dependencies
    private final AutoPipelineRunner autoPipelineRunner;

    // ── POST /api/imports ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public ImportBatchResponse importExcel(MultipartFile file, LocalDate deliveryDate,
                                            boolean confirmReplace, Long uploadedBy) {
        // Step 1: Validate file format (BEFORE transaction — nothing to rollback)
        validateFile(file);

        // Step 2: Parse Excel rows (so size check throws BEFORE creating db batch)
        List<RowData> rows = parseExcelFile(file);


        // Step 4: Create new batch
        ImportBatch batch = ImportBatch.builder()
                .deliveryDate(deliveryDate)
                .fileName(file.getOriginalFilename())
                .uploadedBy(uploadedBy)
                .status("PROCESSING")
                .build();
        batch = batchRepository.save(batch);

        int totalRows = rows.size();
        int acceptedRows = 0;
        int rejectedRows = 0;

        // Cache for stores and products to avoid repeated DB lookups
        Map<String, Store> storeCache = new HashMap<>();
        Map<String, Product> productCache = new HashMap<>();
        // Cache for orders: key = orderRef + "|" + storeId
        Map<String, Order> orderCache = new HashMap<>();
        // Cache for tracking orderRef store mapped in this batch to detect ORDER_REF_STORE_MISMATCH
        Map<String, Store> orderRefStoreCache = new HashMap<>();
        // Cache for tracking orderRef first row number for the error message
        Map<String, Integer> orderRefFirstRow = new HashMap<>();
        // Cache for tracking created order items to accumulate duplicates
        Map<String, OrderItem> orderItemCache = new HashMap<>();
        // Cache for tracking locked trip draft per delivery date
        Map<LocalDate, Boolean> lockedDateCache = new HashMap<>();

        List<ImportError> pendingErrors = new ArrayList<>();
        for (RowData row : rows) {
            try {
                processRow(row, batch, deliveryDate, storeCache, productCache, orderCache, orderRefStoreCache, orderRefFirstRow, orderItemCache, lockedDateCache);
                acceptedRows++;
            } catch (RowRejectedException ex) {
                rejectedRows++;
                ImportError error = ImportError.builder()
                        .importBatch(batch)
                        .rowNumber(row.rowNumber)
                        .rawData(row.toRawString())
                        .errorCode(ex.getErrorCode())
                        .fieldName(ex.getFieldName())
                        .errorReason(ex.getMessage())
                        .build();
                pendingErrors.add(error);
            }
        }
        if (pendingErrors.size() == 1) {
            errorRepository.save(pendingErrors.get(0));
        } else if (pendingErrors.size() > 1) {
            errorRepository.saveAll(pendingErrors);
        }

        // Step 5: Update batch summary
        batch.setTotalRows(totalRows);
        batch.setAcceptedRows(acceptedRows);
        batch.setRejectedRows(rejectedRows);
        batch.setStatus("COMPLETED");
        batchRepository.save(batch);

        long ordersCreated = orderRepository.countByBatchId(batch.getId());

        // ── Step 6: Auto-Pipeline — consolidate → ETA → recommendation ────────
        triggerAutoPipelineForBatch(batch.getId(), deliveryDate);

        return toResponse(batch, ordersCreated);
    }

    /**
     * Auto-Pipeline: after successful import, consolidate trip drafts,
     * calculate ETA, and generate vehicle recommendations.
     * Wrapped in try-catch so import itself is never rolled back.
     */
    private void triggerAutoPipelineForBatch(Long batchId, LocalDate fallbackDate) {
        List<Order> batchOrders = orderRepository.findByImportBatchId(batchId);
        Set<LocalDate> distinctDates = new HashSet<>();
        for (Order order : batchOrders) {
            if (order.getDeliveryDate() != null) {
                distinctDates.add(order.getDeliveryDate());
            }
        }
        if (distinctDates.isEmpty() && fallbackDate != null) {
            distinctDates.add(fallbackDate);
        }

        for (LocalDate deliveryDate : distinctDates) {
            triggerAutoPipeline(deliveryDate);
        }
    }

    private void triggerAutoPipeline(LocalDate deliveryDate) {
        try {
            autoPipelineRunner.runForDate(deliveryDate);
        } catch (Exception ex) {
            log.error("Auto-Pipeline: isolated error for deliveryDate={}: {}",
                    deliveryDate, ex.getMessage(), ex);
        }
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

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ImportErrorResponse>> getBatchErrors(Long batchId, String errorCode, Pageable pageable) {
        // Verify batch exists
        if (!batchRepository.existsById(batchId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Import batch not found: " + batchId, HttpStatus.NOT_FOUND);
        }

        String filterCode = (errorCode == null || errorCode.trim().isEmpty()) ? null : errorCode.trim();
        Page<ImportError> page = errorRepository.findByImportBatchIdAndErrorCode(batchId, filterCode, pageable);

        List<ImportErrorResponse> items = page.getContent().stream()
                .map(e -> ImportErrorResponse.builder()
                        .rowNumber(e.getRowNumber())
                        .rawData(e.getRawData())
                        .errorCode(e.getErrorCode())
                        .fieldName(e.getFieldName())
                        .errorReason(e.getErrorReason())
                        .build())
                .toList();

        return ApiResponse.<List<ImportErrorResponse>>builder()
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

    @Override
    @Transactional(readOnly = true)
    public byte[] exportBatchErrors(Long batchId) {
        if (!batchRepository.existsById(batchId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Import batch not found: " + batchId, HttpStatus.NOT_FOUND);
        }

        try (Workbook workbook = new XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            List<ImportError> errors = errorRepository.findByImportBatchIdOrderByRowNumberAsc(batchId);
            Sheet sheet = workbook.createSheet("Import Errors");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Dòng", "Mã lỗi", "Trường", "Dữ liệu gốc", "Lý do"};
            
            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill data rows
            int rowIdx = 1;
            for (ImportError error : errors) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(error.getRowNumber());
                row.createCell(1).setCellValue(error.getErrorCode());
                row.createCell(2).setCellValue(error.getFieldName() != null ? error.getFieldName() : "");
                row.createCell(3).setCellValue(error.getRawData() != null ? error.getRawData() : "");
                row.createCell(4).setCellValue(error.getErrorReason());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error exporting import batch errors: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Không thể xuất file báo cáo lỗi", HttpStatus.INTERNAL_SERVER_ERROR);
        }
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

            // Pre-check size
            int rowCount = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && !isEmptyRow(row)) {
                    rowCount++;
                }
            }
            if (rowCount > 5000) {
                throw new BusinessException(ErrorCode.FILE_TOO_LARGE,
                        "File Excel vượt quá giới hạn 5.000 dòng dữ liệu", HttpStatus.BAD_REQUEST);
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
                data.deliveryDateRaw = getCellStringValue(row.getCell(4));
                data.deliveryTimeWindow = getCellStringValue(row.getCell(5));
                data.recipientName = getCellStringValue(row.getCell(6));
                data.recipientPhone = getCellStringValue(row.getCell(7));
                data.notes = getCellStringValue(row.getCell(8));
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
                            Map<String, Order> orderCache, Map<String, Store> orderRefStoreCache,
                            Map<String, Integer> orderRefFirstRow, Map<String, OrderItem> orderItemCache,
                            Map<LocalDate, Boolean> lockedDateCache) {

        // Validate required fields (MISSING_FIELD)
        String storeCode = row.storeCode != null ? row.storeCode.trim() : "";
        if (storeCode.isEmpty()) {
            throw new RowRejectedException("Mã cửa hàng không được để trống", "MISSING_FIELD", "store_code");
        }

        String sku = row.sku != null ? row.sku.trim() : "";
        if (sku.isEmpty()) {
            throw new RowRejectedException("SKU không được để trống", "MISSING_FIELD", "sku");
        }

        String quantityRaw = row.quantityRaw != null ? row.quantityRaw.trim() : "";
        if (quantityRaw.isEmpty()) {
            throw new RowRejectedException("Thiếu giá trị Số lượng", "MISSING_FIELD", "quantity");
        }

        String deliveryDateRaw = row.deliveryDateRaw != null ? row.deliveryDateRaw.trim() : "";
        LocalDate rowDeliveryDate = parseRowDate(deliveryDateRaw);
        if (rowDeliveryDate == null && deliveryDate != null) {
            rowDeliveryDate = deliveryDate;
        }
        if (rowDeliveryDate == null) {
            if (deliveryDateRaw.isEmpty()) {
                throw new RowRejectedException("Ngày giao hàng không được để trống", "MISSING_FIELD", "delivery_date");
            } else {
                throw new RowRejectedException("Ngày giao hàng không đúng định dạng DD/MM/YYYY (ví dụ: 02/08/2026) (giá trị: '" + row.deliveryDateRaw + "')", "INVALID_DATE_FORMAT", "delivery_date");
            }

        }

        // Reject rows targeting a delivery date whose TripDraft is already locked (status != DRAFT)
        final LocalDate rowDeliveryDateFinal = rowDeliveryDate;
        boolean dateLocked = lockedDateCache.computeIfAbsent(rowDeliveryDateFinal,
                d -> tripDraftRepository.existsByDeliveryDateAndStatusNot(d, "DRAFT"));
        if (dateLocked) {
            throw new RowRejectedException(
                    "Ngày giao hàng " + rowDeliveryDate + " đã có Trip Draft được xác nhận (không còn ở trạng thái nháp) — không thể nhập/ghi đè dữ liệu cho ngày này. Vui lòng reset Trip Draft trước khi import lại.",
                    "DELIVERY_DATE_LOCKED", "delivery_date");
        }

        // Validate quantity format (INVALID_QUANTITY)
        int quantity;
        try {
            double dVal = Double.parseDouble(quantityRaw);
            if (dVal != Math.floor(dVal) || dVal <= 0) {
                throw new NumberFormatException();
            }
            quantity = (int) dVal;
        } catch (NumberFormatException | NullPointerException e) {
            throw new RowRejectedException("Số lượng phải là số nguyên dương (giá trị: '" + row.quantityRaw + "')", "INVALID_QUANTITY", "quantity");
        }

        // Lookup store (STORE_NOT_FOUND)
        if (!storeCache.containsKey(storeCode)) {
            storeCache.put(storeCode, storeRepository.findByCode(storeCode).orElse(null));
        }
        Store store = storeCache.get(storeCode);
        if (store == null) {
            throw new RowRejectedException("Mã cửa hàng '" + storeCode + "' không tồn tại trong hệ thống", "STORE_NOT_FOUND", "store_code");
        }

        // Lookup product by SKU (SKU_NOT_FOUND, SKU_INACTIVE)
        if (!productCache.containsKey(sku)) {
            productCache.put(sku, productRepository.findBySku(sku).orElse(null));
        }
        Product product = productCache.get(sku);
        if (product == null) {
            throw new RowRejectedException("SKU '" + sku + "' chưa có trong danh mục sản phẩm", "SKU_NOT_FOUND", "sku");
        }
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new RowRejectedException("SKU '" + sku + "' đã ngừng kinh doanh", "SKU_INACTIVE", "sku");
        }

        // Determine order_ref
        String orderRef = (row.orderRef != null && !row.orderRef.trim().isEmpty())
                ? row.orderRef.trim()
                : "AUTO-" + batch.getId() + "-" + row.rowNumber;

        // Check ORDER_REF_STORE_MISMATCH
        if (orderRefStoreCache.containsKey(orderRef)) {
            Store existingStore = orderRefStoreCache.get(orderRef);
            if (!existingStore.getId().equals(store.getId())) {
                Integer firstRow = orderRefFirstRow.get(orderRef);
                throw new RowRejectedException("Mã đơn '" + orderRef + "' đã dùng cho cửa hàng '" + existingStore.getCode() + "' (dòng " + firstRow + ") — không thể dùng lại cho cửa hàng khác", "ORDER_REF_STORE_MISMATCH", "order_ref");
            }
        } else {
            orderRefStoreCache.put(orderRef, store);
            orderRefFirstRow.put(orderRef, row.rowNumber);
        }

        // Find or create Order
        String orderKey = orderRef + "|" + store.getId();
        Order order = orderCache.get(orderKey);
        if (order == null) {
            Optional<Order> existingOrderOpt = orderRepository.findActiveByOrderRefAndDeliveryDate(orderRef, rowDeliveryDate);
            if (existingOrderOpt.isPresent()) {
                Order existingOrder = existingOrderOpt.get();
                existingOrder.setStore(store);
                existingOrder.setDeliveryDate(rowDeliveryDate);
                existingOrder.setImportBatch(batch);
                existingOrder.setDeliveryTimeWindow(row.deliveryTimeWindow);
                existingOrder.setRecipientName(row.recipientName);
                existingOrder.setRecipientPhone(row.recipientPhone);
                existingOrder.setNotes(row.notes);
                order = orderRepository.save(existingOrder);

                // Populate existing order items into cache for cross-batch SKU accumulation
                for (OrderItem existingItem : orderItemRepository.findByOrderId(order.getId())) {
                    if (existingItem.getProduct() != null) {
                        String key = order.getId() + "|" + existingItem.getProduct().getId();
                        orderItemCache.put(key, existingItem);
                    }
                }
            } else {
                Order newOrder = Order.builder()
                        .importBatch(batch)
                        .orderRef(orderRef)
                        .store(store)
                        .deliveryDate(rowDeliveryDate)
                        .deliveryTimeWindow(row.deliveryTimeWindow)
                        .recipientName(row.recipientName)
                        .recipientPhone(row.recipientPhone)
                        .notes(row.notes)
                        .status("ACCEPTED")
                        .build();
                order = orderRepository.save(newOrder);
            }
            orderCache.put(orderKey, order);
        }

        // Create or update OrderItem with snapshot (Accumulation for identical product in same order)
        String orderItemKey = order.getId() + "|" + product.getId();
        BigDecimal unitWeight = product.getWeightKg();
        BigDecimal unitVolume = product.getVolumeM3();

        if (orderItemCache.containsKey(orderItemKey)) {
            OrderItem existingItem = orderItemCache.get(orderItemKey);
            int newQuantity = existingItem.getQuantity() + quantity;
            existingItem.setQuantity(newQuantity);
            BigDecimal newQtyDecimal = BigDecimal.valueOf(newQuantity);
            existingItem.setLineWeightKg(unitWeight.multiply(newQtyDecimal).setScale(3, RoundingMode.HALF_UP));
            existingItem.setLineVolumeM3(unitVolume.multiply(newQtyDecimal).setScale(6, RoundingMode.HALF_UP));
            
            orderItemRepository.save(existingItem);
            log.warn("Dòng trùng SKU '{}' cho đơn hàng '{}'. Tiến hành cộng dồn số lượng. Số lượng mới: {}", sku, orderRef, newQuantity);
        } else {
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
            OrderItem savedItem = orderItemRepository.save(item);
            orderItemCache.put(orderItemKey, savedItem != null ? savedItem : item);
        }
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
                if (DateUtil.isCellDateFormatted(cell)) {
                    try {
                        org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
                        String formatted = formatter.formatCellValue(cell);
                        if (formatted != null && !formatted.isBlank()) {
                            yield formatted.trim();
                        }
                    } catch (Exception ignored) {}

                    try {
                        java.time.LocalDateTime ldt = cell.getLocalDateTimeCellValue();
                        if (ldt != null) {
                            yield ldt.toLocalDate().toString();
                        }
                    } catch (Exception ignored) {}
                }
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
        String uploadedByName = null;
        if (batch.getUploadedBy() != null) {
            uploadedByName = userRepository.findById(batch.getUploadedBy())
                    .map(u -> (u.getFullName() != null && !u.getFullName().isBlank())
                            ? u.getFullName()
                            : u.getUsername())
                    .orElse("User #" + batch.getUploadedBy());
        }

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
                .uploadedBy(uploadedByName)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportedOrderDetailResponse> getImportedOrders(Long batchId) {
        return getImportedOrders(batchId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportedOrderDetailResponse> getImportedOrders(Long batchId, LocalDate deliveryDate) {
        // Verify batch exists
        if (!batchRepository.existsById(batchId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Import batch not found: " + batchId, HttpStatus.NOT_FOUND);
        }

        List<Order> orders = orderRepository.findByImportBatchId(batchId);
        List<ImportedOrderDetailResponse> results = new ArrayList<>();

        for (Order order : orders) {
            if (deliveryDate != null && !deliveryDate.equals(order.getDeliveryDate())) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                results.add(ImportedOrderDetailResponse.builder()
                        .orderRef(order.getOrderRef())
                        .deliveryDate(order.getDeliveryDate())
                        .storeCode(order.getStore().getCode())
                        .storeName(order.getStore().getName())
                        .sku(item.getSku())
                        .productName(item.getProduct() != null ? item.getProduct().getProductName() : null)
                        .quantity(item.getQuantity())
                        .weightKg(item.getLineWeightKg())
                        .volumeM3(item.getLineVolumeM3())
                        .deliveryTimeWindow(order.getDeliveryTimeWindow())
                        .recipientName(order.getRecipientName())
                        .recipientPhone(order.getRecipientPhone())
                        .notes(order.getNotes())
                        .build());
            }
        }
        return results;
    }

    // ── Inner exception for per-row rejection ─────────────────────────────────

    private static class RowRejectedException extends RuntimeException {
        private final String errorCode;
        private final String fieldName;

        RowRejectedException(String message, String errorCode, String fieldName) {
            super(message);
            this.errorCode = errorCode;
            this.fieldName = fieldName;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    // ── Inner class for parsed row data ───────────────────────────────────────

    private static final java.time.format.DateTimeFormatter[] IMPORT_DATE_FORMATTERS = new java.time.format.DateTimeFormatter[]{
            java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("d/M/yy"),
            java.time.format.DateTimeFormatter.ofPattern("d-M-yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("d-M-yy"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    private LocalDate parseRowDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        dateStr = dateStr.trim();

        for (java.time.format.DateTimeFormatter formatter : IMPORT_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ignored) {}
        }

        return null;
    }



    private static class RowData {
        int rowNumber;
        String orderRef;
        String storeCode;
        String sku;
        String quantityRaw;
        String deliveryDateRaw;
        String deliveryTimeWindow;
        String recipientName;
        String recipientPhone;
        String notes;

        String toRawString() {
            return String.join(",", nvl(orderRef), nvl(storeCode), nvl(sku), nvl(quantityRaw), nvl(deliveryDateRaw), nvl(deliveryTimeWindow), nvl(recipientName), nvl(recipientPhone), nvl(notes));
        }

        private String nvl(String s) {
            return s != null ? s : "";
        }
    }

}
