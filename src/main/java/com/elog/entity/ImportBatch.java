package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "total_rows", nullable = false)
    @Builder.Default
    private Integer totalRows = 0;

    @Column(name = "accepted_rows", nullable = false)
    @Builder.Default
    private Integer acceptedRows = 0;

    @Column(name = "rejected_rows", nullable = false)
    @Builder.Default
    private Integer rejectedRows = 0;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PROCESSING";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
