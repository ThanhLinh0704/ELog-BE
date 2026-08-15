package com.elog.integration;

import com.elog.dto.request.StoreCreateRequest;
import com.elog.dto.response.StoreResponse;
import com.elog.entity.District;
import com.elog.entity.Province;
import com.elog.entity.Store;
import com.elog.entity.Ward;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.DistrictRepository;
import com.elog.repository.ProvinceRepository;
import com.elog.repository.StoreRepository;
import com.elog.repository.WardRepository;
import com.elog.service.StoreService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class StoreServiceIntegrationTest {

    @Autowired StoreService storeService;
    @Autowired StoreRepository storeRepository;
    @Autowired ProvinceRepository provinceRepository;
    @Autowired DistrictRepository districtRepository;
    @Autowired WardRepository wardRepository;
    @Autowired EntityManager entityManager;

    @Test
    void l2Sta01CreatesStoreWithPersistedAddressHierarchy() {
        Address address = address("A");
        StoreCreateRequest request = request(address, unique("ST"));

        StoreResponse response = storeService.createStore(request);
        entityManager.flush();
        entityManager.clear();

        Store persisted = storeRepository.findById(response.getId()).orElseThrow();
        assertThat(persisted.getProvince().getCode()).isEqualTo(address.province().getCode());
        assertThat(persisted.getDistrict().getCode()).isEqualTo(address.district().getCode());
        assertThat(persisted.getWard().getCode()).isEqualTo(address.ward().getCode());
    }

    @Test
    void l2Sta02RejectsDistrictFromAnotherProvinceWithoutInsert() {
        Address first = address("B");
        Address second = address("C");
        StoreCreateRequest request = request(first, unique("ST"));
        request.setProvinceCode(second.province().getCode());
        long before = storeRepository.count();

        assertThatThrownBy(() -> storeService.createStore(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_ADDRESS));
        assertThat(storeRepository.count()).isEqualTo(before);
    }

    @Test
    void l2Sta03RejectsDuplicateCodeWithoutInsert() {
        Address address = address("D");
        String code = unique("ST");
        storeService.createStore(request(address, code));
        entityManager.flush();
        long before = storeRepository.count();

        assertThatThrownBy(() -> storeService.createStore(request(address, code)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.STORE_CODE_DUPLICATE));
        assertThat(storeRepository.count()).isEqualTo(before);
    }

    @Test
    void l2Sta04RejectsHalfCoordinatePairWithoutInsert() {
        Address address = address("E");
        StoreCreateRequest request = request(address, unique("ST"));
        request.setLongitude(null);
        long before = storeRepository.count();

        assertThatThrownBy(() -> storeService.createStore(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_COORDINATES));
        assertThat(storeRepository.count()).isEqualTo(before);
    }

    private Address address(String marker) {
        String suffix = marker + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        Province province = provinceRepository.save(Province.builder()
                .code("P" + suffix).name("Province").fullName("Province " + suffix).build());
        District district = districtRepository.save(District.builder()
                .code("D" + suffix).name("District").fullName("District " + suffix).province(province).build());
        Ward ward = wardRepository.save(Ward.builder()
                .code("W" + suffix).name("Ward").fullName("Ward " + suffix).district(district).build());
        return new Address(province, district, ward);
    }

    private StoreCreateRequest request(Address address, String code) {
        StoreCreateRequest request = new StoreCreateRequest();
        request.setStoreCode(code);
        request.setStoreName("Report 5 store");
        request.setProvinceCode(address.province().getCode());
        request.setDistrictCode(address.district().getCode());
        request.setWardCode(address.ward().getCode());
        request.setAddressDetail("123 Report 5 Street");
        request.setLatitude(21.0285);
        request.setLongitude(105.8542);
        return request;
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private record Address(Province province, District district, Ward ward) {}
}

