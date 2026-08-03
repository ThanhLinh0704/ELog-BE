package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_planning_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripPlanningEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_draft_id")
    private Long tripDraftId;

    @Column(name = "trip_id")
    private Long tripId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private PlanningEventType eventType;

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

    @Column(name = "status_before", length = 20)
    private String statusBefore;

    @Column(name = "status_after", length = 20)
    private String statusAfter;

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    @Column(name = "change_detail", columnDefinition = "json")
    private String changeDetail;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "plan_version")
    private Integer planVersion;

    @Column(name = "option_code", length = 100)
    private String optionCode;

    @Column(name = "route_code", length = 20)
    private String routeCode;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;
}
