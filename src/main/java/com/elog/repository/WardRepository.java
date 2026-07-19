package com.elog.repository;

import com.elog.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface WardRepository extends JpaRepository<Ward, String>, JpaSpecificationExecutor<Ward> {
    List<Ward> findByDistrictCode(String districtCode);
}
