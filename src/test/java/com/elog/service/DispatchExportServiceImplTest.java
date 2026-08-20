package com.elog.service;

import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.impl.DispatchExportServiceImpl;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchExportServiceImplTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripDraftRepository tripDraftRepository;

    @Mock
    private TripStopRepository tripStopRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @InjectMocks
    private DispatchExportServiceImpl dispatchExportService;

    private Trip sampleTrip;
    private TripStop sampleTripStop;
    private OrderItem sampleOrderItem;

    @BeforeEach
    void setUp() {
        sampleTrip = buildTrip(10L, 1L, TripStatus.DISPATCHED);

        Province province = Province.builder().code("01").name("Hà Nội").build();
        District district = District.builder().code("001").name("Ba Đình").province(province).build();

        Store store = Store.builder()
                .id(100L)
                .code("STORE01")
                .name("Store Alpha")
                .addressDetail("123 Main St")
                .province(province)
                .district(district)
                .latitude(21.0285)
                .longitude(105.8542)
                .build();

        TripDraftStop draftStop = TripDraftStop.builder()
                .id(1L)
                .store(store)
                .sequenceNo(1)
                .build();

        sampleTripStop = TripStop.builder()
                .tripStopId(101L)
                .trip(sampleTrip)
                .tripDraftStop(draftStop)
                .stopVolumeM3(BigDecimal.valueOf(2.5))
                .stopWeightKg(BigDecimal.valueOf(200.0))
                .sequenceOrder(1)
                .build();

        Order order = Order.builder()
                .id(200L)
                .orderRef("ORD-001")
                .store(store)
                .deliveryDate(LocalDate.of(2026, 8, 18))
                .build();

        Product product = Product.builder()
                .id(300L)
                .sku("SKU-TEST")
                .productName("Test Product")
                .brand("BrandX")
                .productGroup("Electronics")
                .productType("TV")
                .capacityValue(BigDecimal.valueOf(55.0))
                .weightKg(BigDecimal.valueOf(15.0))
                .lengthM(BigDecimal.valueOf(1.2))
                .widthM(BigDecimal.valueOf(0.8))
                .heightM(BigDecimal.valueOf(0.1))
                .volumeM3(BigDecimal.valueOf(0.096))
                .build();

        sampleOrderItem = OrderItem.builder()
                .id(400L)
                .order(order)
                .product(product)
                .sku("SKU-TEST")
                .quantity(2)
                .unitVolumeM3(BigDecimal.valueOf(0.096))
                .unitWeightKg(BigDecimal.valueOf(15.0))
                .lineVolumeM3(BigDecimal.valueOf(0.192))
                .lineWeightKg(BigDecimal.valueOf(30.0))
                .build();
    }

    private Trip buildTrip(Long tripId, Long tripDraftId, TripStatus status) {
        TripDraft draft = TripDraft.builder().id(tripDraftId).build();
        Vehicle vehicle = Vehicle.builder().id(1L).vehicleCode("XE001").plateNumber("29X-123.45")
                .vehicleType("Truck").payloadKg(BigDecimal.valueOf(3500)).maxVolumeM3(BigDecimal.valueOf(19.6))
                .status(VehicleStatus.AVAILABLE).build();
        User driver = User.builder().id(1L).fullName("Nguyen Van A").build();
        Route route = Route.builder().id(1L).code("RT-001").name("Tuyến Hà Nội - Ninh Bình").build();
        return Trip.builder().tripId(tripId).tripDraft(draft).route(route).vehicle(vehicle).driver(driver)
                .deliveryDate(LocalDate.of(2026, 8, 18)).status(status)
                .totalVolumeM3(BigDecimal.valueOf(10)).totalWeightKg(BigDecimal.valueOf(900))
                .stops(List.of()).build();
    }

    @Test
    void exportSingleDispatch_success() throws Exception {
        when(tripDraftRepository.existsById(1L)).thenReturn(true);
        when(tripRepository.findByTripDraftIdAndStatusInWithDetails(eq(1L), anyCollection()))
                .thenReturn(List.of(sampleTrip));
        when(tripStopRepository.findByTripIdInWithStoreForExport(anyCollection()))
                .thenReturn(List.of(sampleTripStop));
        when(orderItemRepository.findByTripDraftIdInWithOrderAndProduct(anyCollection()))
                .thenReturn(List.of(sampleOrderItem));
        when(systemConfigRepository.findByConfigKey("CAPACITY_SAFETY_BUFFER_RATIO"))
                .thenReturn(Optional.of(SystemConfig.builder().configKey("CAPACITY_SAFETY_BUFFER_RATIO").configValue("0.90").build()));

        byte[] result = dispatchExportService.exportSingleDispatch(1L);

        assertThat(result).isNotEmpty();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(9);
            assertThat(wb.getSheetName(0)).isEqualTo("00_Metadata");
            assertThat(wb.getSheetName(1)).isEqualTo("01_Order_Header");
            assertThat(wb.getSheetName(2)).isEqualTo("02_Order_Detail");
            assertThat(wb.getSheetName(3)).isEqualTo("03_Trip_Header");
            assertThat(wb.getSheetName(4)).isEqualTo("04_Trip_Detail");
            assertThat(wb.getSheetName(5)).isEqualTo("05_Vehicles");
            assertThat(wb.getSheetName(6)).isEqualTo("06_Drivers");
            assertThat(wb.getSheetName(7)).isEqualTo("07_Products");
            assertThat(wb.getSheetName(8)).isEqualTo("08_Customers");
        }
    }

    @Test
    void exportSingleDispatch_tripDraftNotFound() {
        when(tripDraftRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> dispatchExportService.exportSingleDispatch(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void exportSingleDispatch_noDispatchedTrips() {
        when(tripDraftRepository.existsById(1L)).thenReturn(true);
        when(tripRepository.findByTripDraftIdAndStatusInWithDetails(eq(1L), anyCollection()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> dispatchExportService.exportSingleDispatch(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_NOT_DISPATCHED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
    }

    @Test
    void exportDispatchByDateRange_success() throws Exception {
        LocalDate fromDate = LocalDate.of(2026, 8, 1);
        LocalDate toDate = LocalDate.of(2026, 8, 18);

        when(tripRepository.findByDeliveryDateBetweenAndStatusInWithDetails(eq(fromDate), eq(toDate), anyCollection()))
                .thenReturn(List.of(sampleTrip));
        when(tripStopRepository.findByTripIdInWithStoreForExport(anyCollection()))
                .thenReturn(List.of(sampleTripStop));
        when(orderItemRepository.findByTripDraftIdInWithOrderAndProduct(anyCollection()))
                .thenReturn(List.of(sampleOrderItem));

        byte[] result = dispatchExportService.exportDispatchByDateRange(fromDate, toDate);

        assertThat(result).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(9);
        }
    }

    @Test
    void exportDispatchByDateRange_invalidRange_fromAfterTo() {
        LocalDate fromDate = LocalDate.of(2026, 8, 20);
        LocalDate toDate = LocalDate.of(2026, 8, 10);

        assertThatThrownBy(() -> dispatchExportService.exportDispatchByDateRange(fromDate, toDate))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
    }

    @Test
    void exportDispatchByDateRange_tooWide() {
        LocalDate fromDate = LocalDate.of(2026, 8, 1);
        LocalDate toDate = LocalDate.of(2026, 9, 10); // > 31 days

        assertThatThrownBy(() -> dispatchExportService.exportDispatchByDateRange(fromDate, toDate))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATE_RANGE_TOO_WIDE)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
    }

    @Test
    void exportDispatchByDateRange_noDataFound() {
        LocalDate fromDate = LocalDate.of(2026, 8, 1);
        LocalDate toDate = LocalDate.of(2026, 8, 5);

        when(tripRepository.findByDeliveryDateBetweenAndStatusInWithDetails(eq(fromDate), eq(toDate), anyCollection()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> dispatchExportService.exportDispatchByDateRange(fromDate, toDate))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }
}
