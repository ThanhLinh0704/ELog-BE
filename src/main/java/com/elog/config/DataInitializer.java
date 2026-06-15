package com.elog.config;

import com.elog.entity.Role;
import com.elog.entity.User;
import com.elog.repository.RoleRepository;
import com.elog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;

/**
 * Initializes default system data (e.g., SYSTEM_ADMIN role and admin user)
 * if they are missing from the database when the application starts up.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Ensure the SYSTEM_ADMIN role exists
        Role adminRole = roleRepository.findByName("SYSTEM_ADMIN")
                .orElseGet(() -> {
                    log.info("Role SYSTEM_ADMIN not found, creating a new one...");
                    Role newRole = Role.builder()
                            .name("SYSTEM_ADMIN")
                            .build();
                    return roleRepository.save(newRole);
                });

        // 2. Check if the default 'admin' user exists
        if (!userRepository.existsByUsername("admin")) {
            log.info("Default admin account 'admin' not found. Initializing default admin...");

            // Check if email is already taken to avoid unique constraint violations
            String email = "admin@elog.vn";
            if (userRepository.existsByEmail(email)) {
                email = "system.admin@elog.vn";
            }

            User adminUser = User.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("Admin@2025"))
                    .fullName("System Administrator")
                    .email(email)
                    .isActive(true)
                    .roles(new HashSet<>(Collections.singletonList(adminRole)))
                    .build();

            userRepository.save(adminUser);
            log.info("Successfully seeded default admin account 'admin' with password 'Admin@2025'");
        } else {
            log.debug("Default admin account already exists. Skipping database seeding.");
        }
    }
}
