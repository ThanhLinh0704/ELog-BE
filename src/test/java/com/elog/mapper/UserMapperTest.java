package com.elog.mapper;

import com.elog.dto.response.user.UserResponse;
import com.elog.entity.LicenseClass;
import com.elog.entity.Role;
import com.elog.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }

    @Test
    void toResponse_shouldMapPhoneNumberAndLicenseClass() {
        Role driverRole = Role.builder().id(1L).name("DRIVER").build();
        User user = User.builder()
                .id(5L)
                .username("driver_user")
                .fullName("Nguyen Van A")
                .email("drivera@elog.vn")
                .phoneNumber("0901234567")
                .licenseClass(LicenseClass.FC)
                .roles(Set.of(driverRole))
                .isActive(true)
                .build();

        UserResponse response = userMapper.toResponse(user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getUsername()).isEqualTo("driver_user");
        assertThat(response.getPhoneNumber()).isEqualTo("0901234567");
        assertThat(response.getLicenseClass()).isEqualTo(LicenseClass.FC);
    }
}
