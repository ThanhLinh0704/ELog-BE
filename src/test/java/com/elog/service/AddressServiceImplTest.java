package com.elog.service;

import com.elog.dto.response.goong.DistrictResponse;
import com.elog.dto.response.goong.ProvinceResponse;
import com.elog.dto.response.goong.WardResponse;
import com.elog.entity.District;
import com.elog.entity.Province;
import com.elog.entity.Ward;
import com.elog.repository.DistrictRepository;
import com.elog.repository.ProvinceRepository;
import com.elog.repository.WardRepository;
import com.elog.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {
    @Mock ProvinceRepository provinceRepository;
    @Mock DistrictRepository districtRepository;
    @Mock WardRepository wardRepository;

    AddressServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AddressServiceImpl(provinceRepository, districtRepository, wardRepository);
    }

    @Test
    @DisplayName("[L1-AD-01] getAllProvinces returns mapped list of provinces")
    void getAllProvincesSuccess() {
        Province p = Province.builder().code("01").name("Hà Nội").fullName("Thành phố Hà Nội").build();
        when(provinceRepository.findAll()).thenReturn(List.of(p));

        List<ProvinceResponse> res = service.getAllProvinces();

        assertAll(
                () -> assertEquals(1, res.size()),
                () -> assertEquals("01", res.getFirst().getCode()),
                () -> assertEquals("Hà Nội", res.getFirst().getName())
        );
    }

    @Test
    @DisplayName("[L1-AD-02] getDistrictsByProvince returns mapped list of districts")
    void getDistrictsByProvinceSuccess() {
        Province p = Province.builder().code("01").name("Hà Nội").build();
        District d = District.builder().code("001").name("Ba Đình").fullName("Quận Ba Đình").province(p).build();
        when(districtRepository.findByProvinceCode("01")).thenReturn(List.of(d));

        List<DistrictResponse> res = service.getDistrictsByProvince("01");

        assertAll(
                () -> assertEquals(1, res.size()),
                () -> assertEquals("001", res.getFirst().getCode()),
                () -> assertEquals("01", res.getFirst().getProvinceCode())
        );
    }

    @Test
    @DisplayName("[L1-AD-03] getWardsByDistrict returns mapped list of wards")
    void getWardsByDistrictSuccess() {
        District d = District.builder().code("001").name("Ba Đình").build();
        Ward w = Ward.builder().code("00001").name("Phúc Xá").fullName("Phường Phúc Xá").district(d).build();
        when(wardRepository.findByDistrictCode("001")).thenReturn(List.of(w));

        List<WardResponse> res = service.getWardsByDistrict("001");

        assertAll(
                () -> assertEquals(1, res.size()),
                () -> assertEquals("00001", res.getFirst().getCode()),
                () -> assertEquals("001", res.getFirst().getDistrictCode())
        );
    }
}
