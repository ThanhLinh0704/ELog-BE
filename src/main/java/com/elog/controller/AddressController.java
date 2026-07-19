package com.elog.controller;

import com.elog.dto.response.*;
import com.elog.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Address", description = "Vietnamese Administrative Units lookup APIs")
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/provinces")
    @Operation(summary = "Get all provinces/cities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getAllProvinces() {
        List<ProvinceResponse> response = addressService.getAllProvinces();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/provinces/{provinceCode}/districts")
    @Operation(summary = "Get districts under a province/city")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> getDistrictsByProvince(
            @PathVariable String provinceCode) {
        List<DistrictResponse> response = addressService.getDistrictsByProvince(provinceCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/districts/{districtCode}/wards")
    @Operation(summary = "Get wards under a district")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<WardResponse>>> getWardsByDistrict(
            @PathVariable String districtCode) {
        List<WardResponse> response = addressService.getWardsByDistrict(districtCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
