package com.elog.controller;

import com.elog.dto.response.goong.DistrictResponse;
import com.elog.dto.response.goong.ProvinceResponse;
import com.elog.dto.response.goong.WardResponse;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController addressController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(addressController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("L3-MASTERDATA-001: GET /api/v1/addresses/districts/{districtCode}/wards - Success returns 200 OK")
    void getWardsByDistrict_Success() throws Exception {
        WardResponse ward = WardResponse.builder().code("W01").name("Phuong Cong Vi").fullName("Phuong Cong Vi, Quan Ba Dinh").districtCode("D01").build();
        when(addressService.getWardsByDistrict("D01")).thenReturn(List.of(ward));

        mockMvc.perform(get("/api/v1/addresses/districts/D01/wards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("W01"));
    }

    @Test
    @DisplayName("L3-MASTERDATA-002: GET /api/v1/addresses/provinces - Success returns 200 OK")
    void getAllProvinces_Success() throws Exception {
        ProvinceResponse province = ProvinceResponse.builder().code("P01").name("Ha Noi").fullName("Thanh pho Ha Noi").build();
        when(addressService.getAllProvinces()).thenReturn(List.of(province));

        mockMvc.perform(get("/api/v1/addresses/provinces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("P01"));
    }

    @Test
    @DisplayName("L3-MASTERDATA-003: GET /api/v1/addresses/provinces/{provinceCode}/districts - Success returns 200 OK")
    void getDistrictsByProvince_Success() throws Exception {
        DistrictResponse district = DistrictResponse.builder().code("D01").name("Ba Dinh").fullName("Quan Ba Dinh").provinceCode("P01").build();
        when(addressService.getDistrictsByProvince("P01")).thenReturn(List.of(district));

        mockMvc.perform(get("/api/v1/addresses/provinces/P01/districts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("D01"));
    }
}
