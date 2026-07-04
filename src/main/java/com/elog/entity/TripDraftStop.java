package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trip_draft_stops",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_tds_draft_stop",
                columnNames = {"trip_draft_id", "route_stop_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripDraftStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_draft_id", nullable = false)
    private TripDraft tripDraft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_stop_id", nullable = false)
    private RouteStop routeStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "order_count", nullable = false)
    @Builder.Default
    private Integer orderCount = 0;
}
