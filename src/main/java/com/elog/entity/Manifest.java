package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "manifests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Manifest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manifest_id")
    private Long manifestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_draft_id", nullable = false, unique = true)
    private TripDraft tripDraft;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "total_lines", nullable = false)
    @Builder.Default
    private Integer totalLines = 0;

    @Column(name = "total_weight_kg", nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal totalWeightKg = BigDecimal.ZERO;

    @Column(name = "total_volume_m3", nullable = false, precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal totalVolumeM3 = BigDecimal.ZERO;

    @OneToMany(mappedBy = "manifest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lifoSequence ASC")
    @Builder.Default
    private List<ManifestLine> lines = new ArrayList<>();
}
