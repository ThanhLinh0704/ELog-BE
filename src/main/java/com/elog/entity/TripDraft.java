package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip_drafts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_td_route_date",
                columnNames = {"route_id", "delivery_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "total_volume_m3", nullable = false, precision = 12, scale = 6)
    @Builder.Default
    private BigDecimal totalVolumeM3 = BigDecimal.ZERO;

    @Column(name = "total_weight_kg", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal totalWeightKg = BigDecimal.ZERO;

    @Column(name = "active_stop_count", nullable = false)
    @Builder.Default
    private Integer activeStopCount = 0;

    @Column(name = "skipped_stop_count", nullable = false)
    @Builder.Default
    private Integer skippedStopCount = 0;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "planned_departure_time")
    private LocalTime plannedDepartureTime;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "volume_check_result", nullable = false, length = 20)
    @Builder.Default
    private ConstraintResult volumeCheckResult = ConstraintResult.NOT_CHECKED;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_check_result", nullable = false, length = 20)
    @Builder.Default
    private ConstraintResult weightCheckResult = ConstraintResult.NOT_CHECKED;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @OneToMany(mappedBy = "tripDraft", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNo ASC")
    @Builder.Default
    private List<TripDraftStop> stops = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
