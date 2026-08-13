package com.elog.integration;

import com.elog.entity.Permission;
import com.elog.entity.Role;
import com.elog.repository.PermissionRepository;
import com.elog.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test-environment repair fixture. The L3 role-permission contract is a real
 * mutation, so a failed/aborted run must not leave the shared local admin role
 * with a reduced permission set.
 */
@SpringBootTest
@ActiveProfiles("dev")
class AdminPermissionFixtureTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    void restoreSystemAdminPermissionsFromCurrentPermissionTable() {
        Role admin = roleRepository.findByName("SYSTEM_ADMIN").orElseThrow();
        List<Permission> allPermissions = permissionRepository.findAll();

        admin.setPermissions(new HashSet<>(allPermissions));
        Role saved = roleRepository.saveAndFlush(admin);

        assertThat(saved.getPermissions())
                .extracting(Permission::getId)
                .containsExactlyInAnyOrderElementsOf(allPermissions.stream().map(Permission::getId).toList());
    }
}
