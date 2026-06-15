package com.elog.repository;

import com.elog.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StoreRepository extends JpaRepository<Store, Long>, JpaSpecificationExecutor<Store> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
