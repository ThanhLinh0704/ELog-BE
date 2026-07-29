package com.elog.repository;

import com.elog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * User repository.
 * findByUsernameAndIsActiveTrue used by UserDetailsServiceImpl for authentication.
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_DRIVER' AND u.isActive = true")
    java.util.List<User> findAllActiveDrivers();
}
