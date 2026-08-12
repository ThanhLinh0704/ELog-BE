package com.elog.service;

import com.elog.dto.response.goong.DistrictResponse;
import com.elog.dto.response.goong.ProvinceResponse;
import com.elog.dto.response.goong.WardResponse;
import java.util.List;

public interface AddressService {
    List<ProvinceResponse> getAllProvinces();
    List<DistrictResponse> getDistrictsByProvince(String provinceCode);
    List<WardResponse> getWardsByDistrict(String districtCode);
}
