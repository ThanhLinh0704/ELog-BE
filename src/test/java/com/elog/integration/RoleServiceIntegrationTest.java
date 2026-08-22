package com.elog.integration;

import com.elog.dto.response.user.RoleResponse;
import com.elog.service.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class RoleServiceIntegrationTest {

    @Autowired
    private RoleService roleService;

    @Test
    void l2Rol01GetsAllRolesWithPermissionsMapping() {
        List<RoleResponse> roles = roleService.getAllRoles();

        assertThat(roles).isNotEmpty();
    }

    @Test
    void l2Rol02GetsRoleByIdWithCascadePermissions() {
        List<RoleResponse> roles = roleService.getAllRoles();
        if (!roles.isEmpty()) {
            RoleResponse first = roles.get(0);
            RoleResponse role = roleService.getRoleById(first.getId());

            assertThat(role).isNotNull();
            assertThat(role.getId()).isEqualTo(first.getId());
        }
    }
}
