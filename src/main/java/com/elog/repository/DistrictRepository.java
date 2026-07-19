package com.elog.repository;

import com.elog.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface DistrictRepository extends JpaRepository<District, String>, JpaSpecificationExecutor<District> {
    List<District> findByProvinceCode(String provinceCode);
}
