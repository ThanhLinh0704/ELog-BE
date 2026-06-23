package com.elog.repository;

import com.elog.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RouteRepository extends JpaRepository<Route, Long>, JpaSpecificationExecutor<Route> {
    boolean existsByCode(String code);
}
