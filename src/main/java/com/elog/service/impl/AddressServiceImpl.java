package com.elog.service.impl;

import com.elog.dto.response.ProvinceResponse;
import com.elog.dto.response.DistrictResponse;
import com.elog.dto.response.WardResponse;
import com.elog.repository.ProvinceRepository;
import com.elog.repository.DistrictRepository;
import com.elog.repository.WardRepository;
import com.elog.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProvinceResponse> getAllProvinces() {
        return provinceRepository.findAll().stream()
                .map(p -> ProvinceResponse.builder()
                        .code(p.getCode())
                        .name(p.getName())
                        .fullName(p.getFullName())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistrictResponse> getDistrictsByProvince(String provinceCode) {
        return districtRepository.findByProvinceCode(provinceCode).stream()
                .map(d -> DistrictResponse.builder()
                        .code(d.getCode())
                        .name(d.getName())
                        .fullName(d.getFullName())
                        .provinceCode(d.getProvince() != null ? d.getProvince().getCode() : null)
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WardResponse> getWardsByDistrict(String districtCode) {
        return wardRepository.findByDistrictCode(districtCode).stream()
                .map(w -> WardResponse.builder()
                        .code(w.getCode())
                        .name(w.getName())
                        .fullName(w.getFullName())
                        .districtCode(w.getDistrict() != null ? w.getDistrict().getCode() : null)
                        .build())
                .toList();
    }
}
