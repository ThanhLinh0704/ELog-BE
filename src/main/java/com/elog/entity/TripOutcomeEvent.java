package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_outcome_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripOutcomeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_execution_id", nullable = false)
    private Long tripExecutionId;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private TripOutcomeEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 30)
    private PlanningActorType actorType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_username", length = 50)
    private String actorUsername;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Column(name = "status_before", length = 30)
    private String statusBefore;

    @Column(name = "status_after", length = 30)
    private String statusAfter;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_ref", length = 50)
    private String orderRef;

    @Column(name = "stop_id")
    private Long stopId;

    @Column(name = "store_code", length = 20)
    private String storeCode;

    @Column(name = "delivery_result", length = 30)
    private String deliveryResult;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(name = "exception_text", length = 1000)
    private String exceptionText;

    @Column(name = "validation_note", length = 1000)
    private String validationNote;

    @Column(name = "route_code", length = 20)
    private String routeCode;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "driver_username", length = 50)
    private String driverUsername;
}
