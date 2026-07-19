package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ward {

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "name_en", length = 255)
    private String nameEn;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "full_name_en", length = 255)
    private String fullNameEn;

    @Column(name = "code_name", length = 255)
    private String codeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_code", referencedColumnName = "code")
    private District district;

    @Column(name = "administrative_unit_id")
    private Integer administrativeUnitId;
}
