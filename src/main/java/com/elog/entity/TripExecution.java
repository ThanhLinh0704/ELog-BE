package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime execution lifecycle of a confirmed trip (FT-09).
 * Status flow: ASSIGNED -> IN_PROGRESS -> COMPLETED / COMPLETED_WITH_EXCEPTIONS / CANCELLED.
 */
@Entity
@Table(name = "trip_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ASSIGNED";

    @Column(name = "assignment_version")
    @Builder.Default
    private Integer assignmentVersion = 1;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "returned_to_warehouse_at")
    private LocalDateTime returnedToWarehouseAt;

    @OneToMany(mappedBy = "tripExecution", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    @Builder.Default
    private List<DeliveryOrderResult> orderResults = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
