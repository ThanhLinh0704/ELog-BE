package com.elog.mapper;

import com.elog.dto.response.vehicle.VehicleListItemResponse;
import com.elog.dto.response.vehicle.VehicleResponse;
import com.elog.entity.LicenseClass;
import com.elog.entity.User;
import com.elog.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleMapperTest {

    private VehicleMapper vehicleMapper;

    @BeforeEach
    void setUp() {
        vehicleMapper = new VehicleMapper();
    }

    @Test
    void toListItem_shouldMapAssignedDriverPhone() {
        User driver = User.builder()
                .id(10L)
                .fullName("Driver Test")
                .phoneNumber("0912345678")
                .licenseClass(LicenseClass.C1)
                .build();

        Vehicle vehicle = Vehicle.builder()
                .id(1L)
                .vehicleCode("V-001")
                .assignedDriver(driver)
                .build();

        VehicleListItemResponse response = vehicleMapper.toListItem(vehicle);

        assertThat(response).isNotNull();
        assertThat(response.getAssignedDriverId()).isEqualTo(10L);
        assertThat(response.getAssignedDriverName()).isEqualTo("Driver Test");
        assertThat(response.getAssignedDriverPhone()).isEqualTo("0912345678");
        assertThat(response.getAssignedDriverLicenseClass()).isEqualTo(LicenseClass.C1);
    }

    @Test
    void toResponse_shouldMapAssignedDriverPhone() {
        User driver = User.builder()
                .id(10L)
                .fullName("Driver Test")
                .phoneNumber("0987654321")
                .licenseClass(LicenseClass.C)
                .build();

        Vehicle vehicle = Vehicle.builder()
                .id(2L)
                .vehicleCode("V-002")
                .assignedDriver(driver)
                .build();

        VehicleResponse response = vehicleMapper.toResponse(vehicle);

        assertThat(response).isNotNull();
        assertThat(response.getAssignedDriverId()).isEqualTo(10L);
        assertThat(response.getAssignedDriverName()).isEqualTo("Driver Test");
        assertThat(response.getAssignedDriverPhone()).isEqualTo("0987654321");
        assertThat(response.getAssignedDriverLicenseClass()).isEqualTo(LicenseClass.C);
    }
}
