package com.elog.service.impl;

import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.DispatchExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Confirmed Dispatch Data Export (Xuất Dữ Liệu Điều Phối Đã Xác Nhận).
 * See filemd/CONFIRMED_DISPATCH_EXPORT_BE_SPEC.md at the repo root for the full design rationale.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchExportServiceImpl implements DispatchExportService {

    private static final List<TripStatus> EXPORTABLE_STATUSES =
            List.of(TripStatus.DISPATCHED, TripStatus.IN_PROGRESS, TripStatus.COMPLETED);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TripRepository tripRepository;
    private final TripDraftRepository tripDraftRepository;
    private final TripStopRepository tripStopRepository;
    private final OrderItemRepository orderItemRepository;
    private final SystemConfigRepository systemConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSingleDispatch(Long tripDraftId) {
        if (!tripDraftRepository.existsById(tripDraftId)) {
            throw new BusinessException(ErrorCode.TRIP_DRAFT_NOT_FOUND,
                    "Trip draft not found: " + tripDraftId, HttpStatus.NOT_FOUND);
        }

        List<Trip> trips = tripRepository.findByTripDraftIdAndStatusInWithDetails(tripDraftId, EXPORTABLE_STATUSES);
        if (trips.isEmpty()) {
            throw new BusinessException(ErrorCode.TRIP_NOT_DISPATCHED,
                    "No dispatched trip found for trip draft " + tripDraftId
                            + " — dispatch must be confirmed (Trip status DISPATCHED or later) before export.",
                    HttpStatus.CONFLICT);
        }

        String scopeLabel = "Single dispatch group (Trip Draft #" + tripDraftId + ")";
        return buildWorkbook(trips, scopeLabel, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDispatchByDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE,
                    "fromDate and toDate are both required", HttpStatus.BAD_REQUEST);
        }
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE,
                    "fromDate must not be after toDate", HttpStatus.BAD_REQUEST);
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) > 31) {
            throw new BusinessException(ErrorCode.DATE_RANGE_TOO_WIDE,
                    "Maximum 31 days per date-range export", HttpStatus.BAD_REQUEST);
        }

        List<Trip> trips = tripRepository.findByDeliveryDateBetweenAndStatusInWithDetails(
                fromDate, toDate, EXPORTABLE_STATUSES);
        if (trips.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "No confirmed dispatch data found between " + fromDate + " and " + toDate,
                    HttpStatus.NOT_FOUND);
        }

        String scopeLabel = "Date range " + fromDate + " to " + toDate;
        return buildWorkbook(trips, scopeLabel, fromDate, toDate);
    }

    // ── Workbook assembly ───────────────────────────────────────────────────

    private byte[] buildWorkbook(List<Trip> trips, String scopeLabel, LocalDate fromDate, LocalDate toDate) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            List<Long> tripIds = trips.stream().map(Trip::getTripId).toList();
            List<Long> tripDraftIds = trips.stream()
                    .map(t -> t.getTripDraft().getId())
                    .distinct()
                    .toList();

            List<TripStop> stops = tripStopRepository.findByTripIdInWithStoreForExport(tripIds);
            List<OrderItem> items = orderItemRepository.findByTripDraftIdInWithOrderAndProduct(tripDraftIds);

            Map<Long, List<TripStop>> stopsByTrip = new LinkedHashMap<>();
            for (TripStop ts : stops) {
                if (ts.getTripDraftStop() == null || ts.getTripDraftStop().getStore() == null) {
                    log.warn("TripStop {} has no linked Store — skipped in export", ts.getTripStopId());
                    continue;
                }
                Long tripId = ts.getTrip().getTripId();
                stopsByTrip.computeIfAbsent(tripId, k -> new ArrayList<>()).add(ts);
            }

            Map<Long, List<OrderItem>> itemsByOrder = new LinkedHashMap<>();
            Map<Long, List<OrderItem>> itemsByStore = new LinkedHashMap<>();
            for (OrderItem oi : items) {
                itemsByOrder.computeIfAbsent(oi.getOrder().getId(), k -> new ArrayList<>()).add(oi);
                itemsByStore.computeIfAbsent(oi.getOrder().getStore().getId(), k -> new ArrayList<>()).add(oi);
            }

            BigDecimal safetyBuffer = getSafetyBufferRatio();

            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle pctStyle = workbook.createCellStyle();
            pctStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0%"));

            writeMetadataSheet(workbook, headerStyle, scopeLabel, fromDate, toDate, trips, itemsByOrder, safetyBuffer);
            writeOrderHeaderSheet(workbook, headerStyle, itemsByOrder);
            writeOrderDetailSheet(workbook, headerStyle, items);
            writeTripHeaderSheet(workbook, headerStyle, pctStyle, trips, stopsByTrip, itemsByStore);
            writeTripDetailSheet(workbook, headerStyle, trips, stopsByTrip, itemsByStore);
            writeVehiclesSheet(workbook, headerStyle, trips);
            writeDriversSheet(workbook, headerStyle, trips);
            writeProductsSheet(workbook, headerStyle, items);
            writeCustomersSheet(workbook, headerStyle, items);

            workbook.write(out);
            return out.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error building confirmed dispatch export: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Không thể xuất file dữ liệu điều phối", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        return headerStyle;
    }

    private BigDecimal getSafetyBufferRatio() {
        // Deliberately mirrors RecommendationServiceImpl.getSafetyBufferRatio() (private there, so
        // duplicated here rather than refactoring an unrelated service to expose it). This value is
        // NOT guaranteed to match what actually gated Trip creation for every historical Trip, since
        // TripServiceImpl uses its own hardcoded 0.90 constant, not this config-driven value — see the
        // Metadata sheet's explicit disclaimer.
        return systemConfigRepository.findByConfigKey("CAPACITY_SAFETY_BUFFER_RATIO")
                .map(c -> {
                    try {
                        return new BigDecimal(c.getConfigValue());
                    } catch (Exception e) {
                        return new BigDecimal("0.90");
                    }
                })
                .orElse(new BigDecimal("0.90"));
    }

    private Row headerRow(Sheet sheet, CellStyle style, String... headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
        return row;
    }

    private void finishSheet(Sheet sheet, int lastRowIdx, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
        if (lastRowIdx >= 0) {
            sheet.setAutoFilter(new CellRangeAddress(0, lastRowIdx, 0, columnCount - 1));
        }
        sheet.createFreezePane(0, 1);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String provinceName(Store store) {
        return store.getProvince() != null ? store.getProvince().getName() : "";
    }

    private String districtName(Store store) {
        return store.getDistrict() != null ? store.getDistrict().getName() : "";
    }

    // ── 00_Metadata ──────────────────────────────────────────────────────────

    private void writeMetadataSheet(Workbook workbook, CellStyle headerStyle, String scopeLabel,
                                     LocalDate fromDate, LocalDate toDate, List<Trip> trips,
                                     Map<Long, List<OrderItem>> itemsByOrder, BigDecimal safetyBuffer) {
        Sheet sheet = workbook.createSheet("00_Metadata");
        String currentUser = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication() != null
                ? org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication().getName()
                : "unknown";

        List<Long> dispatchGroupIds = trips.stream()
                .map(t -> t.getTripDraft().getId()).distinct().sorted().toList();

        Object[][] rows = {
                {"Thời gian xuất", LocalDateTime.now().format(DATETIME_FMT)},
                {"Người xuất", currentUser},
                {"Phạm vi xuất", scopeLabel},
                {"Từ ngày", fromDate != null ? fromDate.format(DATE_FMT) : ""},
                {"Đến ngày", toDate != null ? toDate.format(DATE_FMT) : ""},
                {"Mã nhóm điều phối (Trip Draft ID)", dispatchGroupIds.stream()
                        .map(String::valueOf).collect(Collectors.joining(", "))},
                {"Ngưỡng an toàn áp dụng", safetyBuffer.multiply(BigDecimal.valueOf(100)) + "%"},
                {"Ghi chú ngưỡng an toàn",
                        "Giá trị cấu hình hiện tại của hệ thống (system_config.CAPACITY_SAFETY_BUFFER_RATIO) tại "
                        + "thời điểm export — KHÔNG phải snapshot lịch sử tại thời điểm điều phối, vì hệ thống "
                        + "hiện chưa lưu snapshot ngưỡng an toàn theo từng chuyến."},
                {"Tổng số chuyến", String.valueOf(trips.size())},
                {"Tổng số đơn hàng", String.valueOf(itemsByOrder.size())},
                {"Được tạo bởi", "ELog Dispatch Export Service"},
                {"Ghi chú dữ liệu",
                        "Dữ liệu là bản chụp (snapshot) tại thời điểm export — có thể lệch nếu chuyến hoặc đơn hàng "
                        + "bị chỉnh sửa sau đó. Đây là Operational Data Export, không phải Analytical Report."},
        };

        headerRow(sheet, headerStyle, "Trường", "Giá trị");
        int r = 1;
        for (Object[] pair : rows) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(String.valueOf(pair[0]));
            row.createCell(1).setCellValue(String.valueOf(pair[1]));
        }
        finishSheet(sheet, r - 1, 2);
    }

    // ── 01_Order_Header ──────────────────────────────────────────────────────

    private void writeOrderHeaderSheet(Workbook workbook, CellStyle headerStyle,
                                        Map<Long, List<OrderItem>> itemsByOrder) {
        Sheet sheet = workbook.createSheet("01_Order_Header");
        headerRow(sheet, headerStyle, "Mã đơn hàng", "Ngày giao hàng", "Mã khách hàng", "Tên khách hàng",
                "Địa chỉ giao hàng", "Quận/Huyện", "Tỉnh/Thành phố", "Tổng thể tích (m3)", "Tổng khối lượng (kg)",
                "Số dòng hàng");

        List<Order> orders = itemsByOrder.values().stream()
                .map(list -> list.get(0).getOrder())
                .sorted(Comparator.comparing(Order::getOrderRef))
                .toList();

        int r = 1;
        for (Order order : orders) {
            List<OrderItem> lines = itemsByOrder.get(order.getId());
            BigDecimal totalVolume = lines.stream().map(OrderItem::getLineVolumeM3)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalWeight = lines.stream().map(OrderItem::getLineWeightKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Store store = order.getStore();

            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(order.getOrderRef());
            row.createCell(c++).setCellValue(order.getDeliveryDate() != null ? order.getDeliveryDate().format(DATE_FMT) : "");
            row.createCell(c++).setCellValue(store.getCode());
            row.createCell(c++).setCellValue(store.getName());
            row.createCell(c++).setCellValue(safe(store.getAddressDetail()));
            row.createCell(c++).setCellValue(districtName(store));
            row.createCell(c++).setCellValue(provinceName(store));
            row.createCell(c++).setCellValue(totalVolume.doubleValue());
            row.createCell(c++).setCellValue(totalWeight.doubleValue());
            row.createCell(c).setCellValue(lines.size());
        }
        finishSheet(sheet, r - 1, 10);
    }

    // ── 02_Order_Detail ──────────────────────────────────────────────────────

    private void writeOrderDetailSheet(Workbook workbook, CellStyle headerStyle, List<OrderItem> items) {
        Sheet sheet = workbook.createSheet("02_Order_Detail");
        headerRow(sheet, headerStyle, "Mã đơn hàng", "Mã SKU", "Tên sản phẩm", "Số lượng",
                "Thể tích đơn vị (m3)", "Khối lượng đơn vị (kg)", "Thể tích dòng hàng (m3)", "Khối lượng dòng hàng (kg)");

        List<OrderItem> sorted = items.stream()
                .sorted(Comparator.comparing((OrderItem oi) -> oi.getOrder().getOrderRef())
                        .thenComparing(OrderItem::getSku))
                .toList();

        int r = 1;
        for (OrderItem oi : sorted) {
            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(oi.getOrder().getOrderRef());
            row.createCell(c++).setCellValue(oi.getSku());
            row.createCell(c++).setCellValue(oi.getProduct().getProductName());
            row.createCell(c++).setCellValue(oi.getQuantity());
            row.createCell(c++).setCellValue(oi.getUnitVolumeM3().doubleValue());
            row.createCell(c++).setCellValue(oi.getUnitWeightKg().doubleValue());
            row.createCell(c++).setCellValue(oi.getLineVolumeM3().doubleValue());
            row.createCell(c).setCellValue(oi.getLineWeightKg().doubleValue());
        }
        finishSheet(sheet, r - 1, 8);
    }

    // ── 03_Trip_Header ───────────────────────────────────────────────────────

    private void writeTripHeaderSheet(Workbook workbook, CellStyle headerStyle, CellStyle pctStyle,
                                       List<Trip> trips, Map<Long, List<TripStop>> stopsByTrip,
                                       Map<Long, List<OrderItem>> itemsByStore) {
        Sheet sheet = workbook.createSheet("03_Trip_Header");
        headerRow(sheet, headerStyle, "Mã lệnh", "Ngày lệnh", "Mã xe", "Biển số",
                "Trọng tải (tấn)", "Thể tích thùng (m3)", "Mã lái xe", "Tên lái xe",
                "Mã tuyến", "Tên tuyến", "Số đơn trong lệnh",
                "Tổng thể tích (m3)", "Tổng khối lượng (kg)",
                "% lấp đầy thể tích", "% sử dụng trọng tải");

        int r = 1;
        for (Trip trip : trips) {
            Vehicle vehicle = trip.getVehicle();
            User driver = trip.getDriver();
            Route route = trip.getRoute();
            List<TripStop> stops = stopsByTrip.getOrDefault(trip.getTripId(), List.of());
            int orderCount = stops.stream()
                    .map(ts -> ts.getTripDraftStop().getStore().getId())
                    .distinct()
                    .mapToInt(storeId -> itemsByStore.getOrDefault(storeId, List.of()).stream()
                            .map(oi -> oi.getOrder().getId()).collect(Collectors.toSet()).size())
                    .sum();

            BigDecimal nominalVol = vehicle.getMaxVolumeM3();
            BigDecimal nominalPayload = vehicle.getPayloadKg();
            BigDecimal nominalPayloadTons = nominalPayload.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);

            double volumeFillPct = ratio(trip.getTotalVolumeM3(), nominalVol);
            double payloadUsagePct = ratio(trip.getTotalWeightKg(), nominalPayload);

            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(trip.getTripId());
            row.createCell(c++).setCellValue(trip.getDeliveryDate().format(DATE_FMT));
            row.createCell(c++).setCellValue(vehicle.getVehicleCode());
            row.createCell(c++).setCellValue(vehicle.getPlateNumber());
            row.createCell(c++).setCellValue(nominalPayloadTons.doubleValue());
            row.createCell(c++).setCellValue(nominalVol.doubleValue());
            row.createCell(c++).setCellValue("DRV-" + driver.getId());
            row.createCell(c++).setCellValue(driver.getFullName());
            row.createCell(c++).setCellValue(route != null ? safe(route.getCode()) : "");
            row.createCell(c++).setCellValue(route != null ? safe(route.getName()) : "");
            row.createCell(c++).setCellValue(orderCount);
            row.createCell(c++).setCellValue(trip.getTotalVolumeM3().doubleValue());
            row.createCell(c++).setCellValue(trip.getTotalWeightKg().doubleValue());
            setPercentCell(row.createCell(c++), volumeFillPct, pctStyle);
            setPercentCell(row.createCell(c), payloadUsagePct, pctStyle);
        }
        finishSheet(sheet, r - 1, 15);
    }

    private double ratio(BigDecimal used, BigDecimal capacity) {
        if (capacity == null || capacity.compareTo(BigDecimal.ZERO) == 0) return 0d;
        return used.divide(capacity, 4, RoundingMode.HALF_UP).doubleValue();
    }

    private void setPercentCell(Cell cell, double ratio, CellStyle pctStyle) {
        cell.setCellValue(ratio); // POI percent format multiplies by 100 for display
        cell.setCellStyle(pctStyle);
    }

    // ── 04_Trip_Detail ───────────────────────────────────────────────────────

    private void writeTripDetailSheet(Workbook workbook, CellStyle headerStyle, List<Trip> trips,
                                       Map<Long, List<TripStop>> stopsByTrip,
                                       Map<Long, List<OrderItem>> itemsByStore) {
        Sheet sheet = workbook.createSheet("04_Trip_Detail");
        headerRow(sheet, headerStyle, "Mã chuyến", "Thứ tự giao hàng", "Mã đơn hàng", "Mã khách hàng",
                "Tên khách hàng", "Địa chỉ giao hàng", "Quận/Huyện", "Tỉnh/Thành phố",
                "Thể tích điểm dừng (m3)", "Khối lượng điểm dừng (kg)");

        int r = 1;
        for (Trip trip : trips) {
            int stopSeq = 0;
            for (TripStop ts : stopsByTrip.getOrDefault(trip.getTripId(), List.of())) {
                stopSeq++;
                Store store = ts.getTripDraftStop().getStore();
                List<OrderItem> storeItems = itemsByStore.getOrDefault(store.getId(), List.of());
                Map<Long, List<OrderItem>> byOrder = storeItems.stream()
                        .collect(Collectors.groupingBy(oi -> oi.getOrder().getId(), LinkedHashMap::new, Collectors.toList()));

                for (List<OrderItem> orderLines : byOrder.values()) {
                    Order order = orderLines.get(0).getOrder();
                    Row row = sheet.createRow(r++);
                    int c = 0;
                    row.createCell(c++).setCellValue(trip.getTripId());
                    row.createCell(c++).setCellValue(stopSeq);
                    row.createCell(c++).setCellValue(order.getOrderRef());
                    row.createCell(c++).setCellValue(store.getCode());
                    row.createCell(c++).setCellValue(store.getName());
                    row.createCell(c++).setCellValue(safe(store.getAddressDetail()));
                    row.createCell(c++).setCellValue(districtName(store));
                    row.createCell(c++).setCellValue(provinceName(store));
                    row.createCell(c++).setCellValue(ts.getStopVolumeM3().doubleValue());
                    row.createCell(c).setCellValue(ts.getStopWeightKg().doubleValue());
                }
            }
        }
        finishSheet(sheet, r - 1, 10);
    }

    // ── 05_Vehicles (distinct, in-scope only) ───────────────────────────────

    private void writeVehiclesSheet(Workbook workbook, CellStyle headerStyle, List<Trip> trips) {
        Sheet sheet = workbook.createSheet("05_Vehicles");
        headerRow(sheet, headerStyle, "Mã xe", "Biển số xe",
                "Tải trọng danh nghĩa (kg)", "Thể tích danh nghĩa (m3)", "Kích thước khoang hàng D x R x C (mm)",
                "Bằng lái yêu cầu", "Trạng thái");

        List<Vehicle> vehicles = trips.stream().map(Trip::getVehicle)
                .collect(Collectors.toMap(Vehicle::getId, v -> v, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(Vehicle::getVehicleCode))
                .toList();

        int r = 1;
        for (Vehicle v : vehicles) {
            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(v.getVehicleCode());
            row.createCell(c++).setCellValue(v.getPlateNumber());
            row.createCell(c++).setCellValue(v.getPayloadKg().doubleValue());
            row.createCell(c++).setCellValue(v.getMaxVolumeM3().doubleValue());
            row.createCell(c++).setCellValue(
                    safe(v.getCargoLengthMm() == null ? null : String.valueOf(v.getCargoLengthMm())) + " x "
                    + safe(v.getCargoWidthMm() == null ? null : String.valueOf(v.getCargoWidthMm())) + " x "
                    + safe(v.getCargoHeightMm() == null ? null : String.valueOf(v.getCargoHeightMm())));
            row.createCell(c++).setCellValue(v.getRequiredLicense() != null ? v.getRequiredLicense().name() : "");
            row.createCell(c).setCellValue(v.getStatus().name());
        }
        finishSheet(sheet, r - 1, 7);
    }

    // ── 06_Drivers (distinct, in-scope only) ────────────────────────────────

    private void writeDriversSheet(Workbook workbook, CellStyle headerStyle, List<Trip> trips) {
        Sheet sheet = workbook.createSheet("06_Drivers");
        headerRow(sheet, headerStyle, "Mã tài xế", "Tên tài xế", "Hạng bằng lái", "Trạng thái tài xế", "Số điện thoại");

        List<User> drivers = trips.stream().map(Trip::getDriver)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(User::getFullName))
                .toList();

        int r = 1;
        for (User d : drivers) {
            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue("DRV-" + d.getId());
            row.createCell(c++).setCellValue(d.getFullName());
            row.createCell(c++).setCellValue(d.getLicenseClass() != null ? d.getLicenseClass().name() : "");
            row.createCell(c++).setCellValue(d.getDriverStatus() != null ? d.getDriverStatus().name() : "");
            // Phone is PII — currently shown to all callers of this endpoint since the endpoint itself is
            // gated to trip:coordinate/trip:read (see controller). Tighten here if a broader-audience role
            // is later granted access to this endpoint.
            row.createCell(c).setCellValue(safe(d.getPhoneNumber()));
        }
        finishSheet(sheet, r - 1, 5);
    }

    // ── 07_Products (distinct, in-scope only) ───────────────────────────────

    private void writeProductsSheet(Workbook workbook, CellStyle headerStyle, List<OrderItem> items) {
        Sheet sheet = workbook.createSheet("07_Products");
        headerRow(sheet, headerStyle, "Mã SKU", "Tên sản phẩm", "Thương hiệu", "Nhóm hàng", "Loại hàng hóa",
                "Dung tích (L) / KL giặt (kg)", "Khối lượng (kg)", "Chiều dài (m)", "Chiều rộng (m)",
                "Chiều cao (m)", "Thể tích (m3)");

        List<Product> products = items.stream().map(OrderItem::getProduct)
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(Product::getSku))
                .toList();

        int r = 1;
        for (Product p : products) {
            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(p.getSku());
            row.createCell(c++).setCellValue(p.getProductName());
            row.createCell(c++).setCellValue(safe(p.getBrand()));
            row.createCell(c++).setCellValue(safe(p.getProductGroup()));
            row.createCell(c++).setCellValue(safe(p.getProductType()));
            if (p.getCapacityValue() != null) row.createCell(c++).setCellValue(p.getCapacityValue().doubleValue()); else c++;
            row.createCell(c++).setCellValue(p.getWeightKg().doubleValue());
            row.createCell(c++).setCellValue(p.getLengthM().doubleValue());
            row.createCell(c++).setCellValue(p.getWidthM().doubleValue());
            row.createCell(c++).setCellValue(p.getHeightM().doubleValue());
            row.createCell(c).setCellValue(p.getVolumeM3().doubleValue());
        }
        finishSheet(sheet, r - 1, 11);
    }

    // ── 08_Customers (distinct Store, in-scope only) ────────────────────────

    private void writeCustomersSheet(Workbook workbook, CellStyle headerStyle, List<OrderItem> items) {
        Sheet sheet = workbook.createSheet("08_Customers");
        headerRow(sheet, headerStyle, "Mã khách hàng/cửa hàng", "Tên", "Địa chỉ", "Quận/Huyện", "Tỉnh/Thành phố",
                "Vĩ độ", "Kinh độ");

        List<Store> stores = items.stream().map(oi -> oi.getOrder().getStore())
                .collect(Collectors.toMap(Store::getId, s -> s, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(Store::getCode))
                .toList();

        int r = 1;
        for (Store s : stores) {
            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(s.getCode());
            row.createCell(c++).setCellValue(s.getName());
            row.createCell(c++).setCellValue(safe(s.getAddressDetail()));
            row.createCell(c++).setCellValue(districtName(s));
            row.createCell(c++).setCellValue(provinceName(s));
            if (s.getLatitude() != null) row.createCell(c++).setCellValue(s.getLatitude()); else c++;
            if (s.getLongitude() != null) row.createCell(c).setCellValue(s.getLongitude());
        }
        finishSheet(sheet, r - 1, 7);
    }
}
