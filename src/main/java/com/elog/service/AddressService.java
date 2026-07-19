package com.elog.service;

import com.elog.dto.response.ProvinceResponse;
import com.elog.dto.response.DistrictResponse;
import com.elog.dto.response.WardResponse;
import java.util.List;

public interface AddressService {
    List<ProvinceResponse> getAllProvinces();
    List<DistrictResponse> getDistrictsByProvince(String provinceCode);
    List<WardResponse> getWardsByDistrict(String districtCode);
}
