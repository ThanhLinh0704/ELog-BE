package com.elog.repository;

import com.elog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User repository.
 * findByUsernameAndIsActiveTrue used by UserDetailsServiceImpl for authentication.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndIsActiveTrue(String username);

    boolean existsByUsername(String username);
}
