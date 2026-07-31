package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Terminal/non-terminal delivery status for each order at a stop (FT-09).
 * Statuses: PENDING (non-terminal), DELIVERED, PARTIALLY_DELIVERED, FAILED, CANCELLED.
 */
@Entity
@Table(name = "delivery_order_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryOrderResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_execution_id", nullable = false)
    private TripExecution tripExecution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private TripDraftStop stop;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
