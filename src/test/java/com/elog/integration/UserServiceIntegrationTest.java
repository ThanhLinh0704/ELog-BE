package com.elog.integration;

import com.elog.dto.request.user.UserCreateRequest;
import com.elog.dto.request.user.UserStatusUpdateRequest;
import com.elog.dto.response.user.UserResponse;
import com.elog.entity.Role;
import com.elog.entity.User;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.RoleRepository;
import com.elog.repository.UserRepository;
import com.elog.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class UserServiceIntegrationTest {

    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;

    @Test
    void l2Udr01CreatesUserWithEncodedPasswordAndPersistedRoleLink() {
        Role role = roleRepository.findByName("DISPATCHER").orElseThrow();
        UserCreateRequest request = request("DISPATCHER");

        UserResponse response = userService.createUser(request);
        entityManager.flush();
        entityManager.clear();

        User persisted = userRepository.findById(response.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("R5-password1!", persisted.getPasswordHash())).isTrue();
        assertThat(persisted.getRoles()).extracting(Role::getId).containsExactly(role.getId());
    }

    @Test
    void l2Udr02DuplicateUsernameDoesNotInsertUser() {
        User existing = user(true);
        UserCreateRequest request = request("DISPATCHER");
        request.setUsername(existing.getUsername());
        long before = userRepository.count();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(userRepository.count()).isEqualTo(before);
    }

    @Test
    void l2Udr03DuplicateEmailDoesNotInsertUser() {
        User existing = user(true);
        UserCreateRequest request = request("DISPATCHER");
        request.setEmail(existing.getEmail());
        long before = userRepository.count();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(userRepository.count()).isEqualTo(before);
    }

    @Test
    void l2Udr04PreventsSelfLockoutAndLeavesPersistedStatusActive() {
        User admin = user(true);
        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setIsActive(false);

        assertThatThrownBy(() -> userService.updateUserStatus(admin.getId(), request, admin.getUsername()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        entityManager.clear();
        assertThat(userRepository.findById(admin.getId()).orElseThrow().getIsActive()).isTrue();
    }

    private UserCreateRequest request(String role) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("r5usr" + suffix);
        request.setEmail("r5usr" + suffix + "@example.test");
        request.setFullName("Report 5 User");
        request.setPassword("R5-password1!");
        request.setRoles(Set.of(role));
        return request;
    }

    private User user(boolean active) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.saveAndFlush(User.builder()
                .username("r5existing" + suffix)
                .email("r5existing" + suffix + "@example.test")
                .fullName("Report 5 Existing")
                .passwordHash(passwordEncoder.encode("R5-password1!"))
                .isActive(active)
                .build());
    }
}

