package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_code", nullable = false, unique = true, length = 50)
    private String vehicleCode;

    @Column(name = "plate_number", nullable = false, unique = true, length = 20)
    private String plateNumber;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType;

    @Column(name = "vehicle_class", length = 20)
    private String vehicleClass;

    @Column(name = "payload_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal payloadKg;

    @Column(name = "gross_vehicle_weight_kg", precision = 10, scale = 2)
    private BigDecimal grossVehicleWeightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_license", length = 10)
    private LicenseClass requiredLicense;

    @Column(name = "max_volume_m3", nullable = false, precision = 8, scale = 3)
    private BigDecimal maxVolumeM3;

    @Column(name = "cargo_length_mm")
    private Integer cargoLengthMm;

    @Column(name = "cargo_width_mm")
    private Integer cargoWidthMm;

    @Column(name = "cargo_height_mm")
    private Integer cargoHeightMm;

    @Column(name = "average_speed_kmh", precision = 5, scale = 2)
    private BigDecimal averageSpeedKmh;

    @Column(name = "cost_per_km", precision = 10, scale = 2)
    private BigDecimal costPerKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "permit_info", columnDefinition = "TEXT")
    private String permitInfo;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
