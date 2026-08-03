package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_stops",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ts_trip_stop", columnNames = {"trip_id", "route_stop_id"}),
                @UniqueConstraint(name = "uq_ts_trip_seq", columnNames = {"trip_id", "sequence_order"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_stop_id")
    private Long tripStopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_stop_id", nullable = false)
    private RouteStop routeStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_draft_stop_id", nullable = false)
    private TripDraftStop tripDraftStop;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "planned_eta")
    private LocalDateTime plannedEta;

    @Column(name = "actual_arrival_time")
    private LocalDateTime actualArrivalTime;

    @Column(name = "actual_departure_time")
    private LocalDateTime actualDepartureTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TripStopStatus status = TripStopStatus.PENDING;

    @Column(name = "stop_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal stopWeightKg;

    @Column(name = "stop_volume_m3", nullable = false, precision = 10, scale = 6)
    private BigDecimal stopVolumeM3;

    @Column(name = "notes")
    private String notes;

    @Column(name = "planned_waiting_time_min")
    private Integer plannedWaitingTimeMin;

    @Column(name = "violation_code", length = 50)
    private String violationCode;

    @Column(name = "distance_from_prev_km", precision = 10, scale = 2)
    private BigDecimal distanceFromPrevKm;

    @Column(name = "travel_time_from_prev_min")
    private Integer travelTimeFromPrevMin;
}

