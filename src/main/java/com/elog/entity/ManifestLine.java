package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "manifest_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManifestLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manifest_id", nullable = false)
    private Manifest manifest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_draft_stop_id", nullable = false)
    private TripDraftStop tripDraftStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "lifo_sequence", nullable = false)
    private Integer lifoSequence;

    @Column(name = "stop_sequence_no", nullable = false)
    private Integer stopSequenceNo;

    @Column(name = "store_code", nullable = false, length = 20)
    private String storeCode;

    @Column(name = "store_name", nullable = false, length = 100)
    private String storeName;

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_volume_m3", nullable = false, precision = 10, scale = 6)
    private BigDecimal unitVolumeM3;

    @Column(name = "unit_weight_kg", nullable = false, precision = 8, scale = 3)
    private BigDecimal unitWeightKg;

    @Column(name = "line_volume_m3", nullable = false, precision = 10, scale = 6)
    private BigDecimal lineVolumeM3;

    @Column(name = "line_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal lineWeightKg;
}
