package com.elog.integration;

import com.elog.dto.request.VehicleCreateRequest;
import com.elog.dto.response.VehicleResponse;
import com.elog.entity.LicenseClass;
import com.elog.entity.User;
import com.elog.entity.Vehicle;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.UserRepository;
import com.elog.repository.VehicleRepository;
import com.elog.service.VehicleService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class VehicleServiceIntegrationTest {

    @Autowired VehicleService vehicleService;
    @Autowired VehicleRepository vehicleRepository;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager entityManager;

    @Test
    void l2Vpm01CreatesNormalizedVehicleAndLinksPersistedDriver() {
        User driver = userRepository.findByUsername("driver01").orElseThrow();
        VehicleCreateRequest request = validRequest();
        request.setAssignedDriverId(driver.getId());

        VehicleResponse response = vehicleService.createVehicle(request);
        entityManager.flush();
        entityManager.clear();

        Vehicle persisted = vehicleRepository.findById(response.getId()).orElseThrow();
        assertThat(persisted.getPlateNumber()).isEqualTo(request.getPlateNumber().toUpperCase());
        assertThat(persisted.getAssignedDriver().getId()).isEqualTo(driver.getId());
    }

    @Test
    void l2Vpm02RejectsDuplicateNormalizedPlateWithoutInsert() {
        VehicleCreateRequest first = validRequest();
        vehicleService.createVehicle(first);
        entityManager.flush();
        long before = vehicleRepository.count();
        VehicleCreateRequest duplicate = validRequest();
        duplicate.setPlateNumber(first.getPlateNumber().toLowerCase());

        assertThatThrownBy(() -> vehicleService.createVehicle(duplicate))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.VEHICLE_PLATE_DUPLICATE));
        assertThat(vehicleRepository.count()).isEqualTo(before);
    }

    @Test
    void l2Vpm03RejectsDuplicateVehicleCodeWithoutInsert() {
        VehicleCreateRequest first = validRequest();
        vehicleService.createVehicle(first);
        entityManager.flush();
        long before = vehicleRepository.count();
        VehicleCreateRequest duplicate = validRequest();
        duplicate.setVehicleCode(first.getVehicleCode());

        assertThatThrownBy(() -> vehicleService.createVehicle(duplicate))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.VEHICLE_CODE_DUPLICATE));
        assertThat(vehicleRepository.count()).isEqualTo(before);
    }

    @Test
    void l2Vpm04RejectsInvalidCapacityRatioWithoutInsert() {
        VehicleCreateRequest request = validRequest();
        request.setPayloadKg(new BigDecimal("10.00"));
        request.setMaxVolumeM3(new BigDecimal("10.000"));
        long before = vehicleRepository.count();

        assertThatThrownBy(() -> vehicleService.createVehicle(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_CAPACITY_RATIO));
        assertThat(vehicleRepository.count()).isEqualTo(before);
    }

    private VehicleCreateRequest validRequest() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return VehicleCreateRequest.builder()
                .vehicleCode("R5V-" + suffix)
                .plateNumber("R5-" + suffix)
                .vehicleType("Truck")
                .vehicleClass("Light")
                .payloadKg(new BigDecimal("3000.00"))
                .grossVehicleWeightKg(new BigDecimal("5000.00"))
                .requiredLicense(LicenseClass.B)
                .maxVolumeM3(new BigDecimal("10.000"))
                .build();
    }
}

