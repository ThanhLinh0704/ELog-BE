package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * System-generated post-trip summary submitted by driver for Dispatcher validation (FT-09).
 * Statuses: SUBMITTED, VALIDATED, NEEDS_CORRECTION.
 */
@Entity
@Table(name = "trip_outcomes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_execution_id", nullable = false)
    private TripExecution tripExecution;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "SUBMITTED";

    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(name = "delivered_count")
    private Integer deliveredCount;

    @Column(name = "failed_count")
    private Integer failedCount;

    @Column(name = "partial_count")
    private Integer partialCount;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "amendment_reason", columnDefinition = "TEXT")
    private String amendmentReason;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
