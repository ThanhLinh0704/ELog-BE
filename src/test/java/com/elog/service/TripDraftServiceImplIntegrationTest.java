package com.elog.service;

import com.elog.dto.response.*;
import com.elog.entity.*;
import com.elog.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class TripDraftServiceImplIntegrationTest {

    @Autowired
    private TripDraftService tripDraftService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteStopRepository routeStopRepository;

    @Autowired
    private ImportBatchRepository importBatchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripDraftStopRepository tripDraftStopRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testConsolidateIntegrationSuccess() {
        // Use a unique delivery date to prevent uq_batch_active_date conflicts
        LocalDate deliveryDate = LocalDate.now().plusYears(10).plusDays(new java.util.Random().nextInt(1000));
        
        // Deactivate any existing active batch for this date just in case
        importBatchRepository.findActiveByDate(deliveryDate).ifPresent(b -> {
            b.setIsActive(false);
            importBatchRepository.saveAndFlush(b);
        });

        // Find default admin user seeded by DataInitializer
        User adminUser = userRepository.findByUsername("admin")
                .orElseThrow(() -> new IllegalStateException("Admin user not found"));

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Route
        Route route = Route.builder()
                .code("RT-INT-" + uniqueSuffix)
                .name("Integration Route " + uniqueSuffix)
                .isActive(true)
                .build();
        route = routeRepository.save(route);

        // 2. Create Store
        Store store = Store.builder()
                .code("ST-INT-" + uniqueSuffix)
                .name("Integration Store " + uniqueSuffix)
                .isActive(true)
                .address("123 Test St")
                .build();
        store = storeRepository.save(store);

        // 3. Create RouteStop
        RouteStop routeStop = RouteStop.builder()
                .route(route)
                .store(store)
                .sequenceOrder(1)
                .build();
        routeStop = routeStopRepository.save(routeStop);

        // 4. Create Product
        Product product = Product.builder()
                .sku("SKU-INT-" + uniqueSuffix)
                .productName("Integration Product " + uniqueSuffix)
                .weightKg(BigDecimal.valueOf(10.0))
                .lengthM(BigDecimal.valueOf(1.0))
                .widthM(BigDecimal.valueOf(1.0))
                .heightM(BigDecimal.valueOf(1.0))
                .volumeM3(BigDecimal.valueOf(1.0))
                .isActive(true)
                .build();
        product = productRepository.save(product);

        // 5. Create ImportBatch
        ImportBatch batch = ImportBatch.builder()
                .deliveryDate(deliveryDate)
                .fileName("test_" + uniqueSuffix + ".xlsx")
                .uploadedBy(adminUser.getId())
                .totalRows(1)
                .acceptedRows(1)
                .status("COMPLETED")
                .isActive(true)
                .build();
        batch = importBatchRepository.save(batch);

        // 6. Create Order
        Order order = Order.builder()
                .importBatch(batch)
                .orderRef("ORD-INT-" + uniqueSuffix)
                .store(store)
                .deliveryDate(deliveryDate)
                .status("ACCEPTED")
                .build();
        order = orderRepository.save(order);

        // 7. Create OrderItem
        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .sku(product.getSku())
                .quantity(2)
                .unitWeightKg(product.getWeightKg())
                .unitVolumeM3(product.getVolumeM3())
                .lineWeightKg(BigDecimal.valueOf(20.0))
                .lineVolumeM3(BigDecimal.valueOf(2.0))
                .build();
        orderItemRepository.save(orderItem);

        // Add item to order's list (JPA relationship)
        order.getItems().add(orderItem);

        // Flush database writes
        entityManager.flush();

        // 8. Run consolidation
        ConsolidateResponse response = tripDraftService.consolidate(deliveryDate);

        // Flush consolidation updates
        entityManager.flush();
        // Clear L1 Cache to force loading from database
        entityManager.clear();

        // 9. Asserts
        assertThat(response).isNotNull();
        assertThat(response.getTripDraftsCreatedOrUpdated()).isEqualTo(1);
        assertThat(response.getTripDrafts()).hasSize(1);

        TripDraftResponse draftResponse = response.getTripDrafts().get(0);
        assertThat(draftResponse.getRouteCode()).isEqualTo(route.getCode());
        assertThat(draftResponse.getTotalWeightKg()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        assertThat(draftResponse.getTotalVolumeM3()).isEqualByComparingTo(BigDecimal.valueOf(2.0));
        assertThat(draftResponse.getActiveStopCount()).isEqualTo(1);
        assertThat(draftResponse.getSkippedStopCount()).isZero();

        // 10. Check if order is updated with tripDraftId
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getTripDraft()).isNotNull();
        assertThat(updatedOrder.getTripDraft().getId()).isEqualTo(draftResponse.getId());

        // 11. Check trip draft stops in DB
        List<TripDraftStop> stops = tripDraftStopRepository.findAll();
        assertThat(stops).isNotEmpty();
        TripDraftStop stop = stops.stream()
                .filter(s -> s.getTripDraft().getId().equals(draftResponse.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(stop.getStore().getCode()).isEqualTo(store.getCode());
        assertThat(stop.getIsActive()).isTrue();
        assertThat(stop.getOrderCount()).isEqualTo(1);
    }
}
