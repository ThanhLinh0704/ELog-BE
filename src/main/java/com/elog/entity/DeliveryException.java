package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Ghi nhận ngoại lệ vận hành — 2 loại:
 * - TIME_EXCEPTION: Hệ thống tự động tạo khi stop quá ETA (BR-09) — US-17
 * - DELIVERY_REJECTION: Driver ghi nhận cửa hàng từ chối nhận hàng (BR-10) —
 * US-18
 *
 * reported_by = 1 (System User) cho TIME_EXCEPTION tự động
 * reported_by = driver.id cho DELIVERY_REJECTION
 */
@Entity
@Table(name = "delivery_exceptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exception_id")
    private Long exceptionId;

    @Column(name = "trip_stop_id")
    private Long tripStopId;

    @Column(name = "trip_execution_id")
    private Long tripExecutionId;

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 30)
    private ExceptionType exceptionType;

    @Column(name = "reported_by", nullable = false)
    private Long reportedBy;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
}
