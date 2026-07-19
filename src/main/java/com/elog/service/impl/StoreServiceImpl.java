package com.elog.service.impl;

import com.elog.dto.request.*;
import com.elog.dto.response.*;
import com.elog.entity.Store;
import com.elog.entity.Province;
import com.elog.entity.District;
import com.elog.entity.Ward;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.StoreMapper;
import com.elog.repository.RouteStopRepository;
import com.elog.repository.StoreRepository;
import com.elog.repository.ProvinceRepository;
import com.elog.repository.DistrictRepository;
import com.elog.repository.WardRepository;
import com.elog.repository.specification.StoreSpecification;
import com.elog.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final RouteStopRepository routeStopRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final StoreMapper storeMapper;

    @Override
    @Transactional
    public StoreResponse createStore(StoreCreateRequest request) {
        if (storeRepository.existsByCode(request.getStoreCode())) {
            throw new BusinessException(ErrorCode.STORE_CODE_DUPLICATE,
                    "Store code already exists: " + request.getStoreCode(), HttpStatus.CONFLICT);
        }
        validateCoordinates(request.getLatitude(), request.getLongitude());

        Province province = provinceRepository.findById(request.getProvinceCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVINCE_NOT_FOUND, "Province not found: " + request.getProvinceCode(), HttpStatus.BAD_REQUEST));
        District district = districtRepository.findById(request.getDistrictCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.DISTRICT_NOT_FOUND, "District not found: " + request.getDistrictCode(), HttpStatus.BAD_REQUEST));
        Ward ward = wardRepository.findById(request.getWardCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.WARD_NOT_FOUND, "Ward not found: " + request.getWardCode(), HttpStatus.BAD_REQUEST));

        if (district.getProvince() == null || !district.getProvince().getCode().equals(province.getCode())) {
            throw new BusinessException(ErrorCode.INVALID_ADDRESS, "District does not belong to the selected Province", HttpStatus.BAD_REQUEST);
        }
        if (ward.getDistrict() == null || !ward.getDistrict().getCode().equals(district.getCode())) {
            throw new BusinessException(ErrorCode.INVALID_ADDRESS, "Ward does not belong to the selected District", HttpStatus.BAD_REQUEST);
        }

        Store store = storeMapper.toEntity(request);
        store.setProvince(province);
        store.setDistrict(district);
        store.setWard(ward);
        Store saved = storeRepository.save(store);
        return buildStoreResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Long id) {
        Store store = findStoreOrThrow(id);
        return buildStoreResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<StoreListItemResponse>> getAllStores(
            String keyword, Boolean isActive, Boolean hasRoute, Pageable pageable) {

        Specification<Store> spec = Specification.where(StoreSpecification.hasKeyword(keyword))
                .and(StoreSpecification.hasActiveStatus(isActive))
                .and(StoreSpecification.hasRoute(hasRoute));

        Page<Store> page = storeRepository.findAll(spec, pageable);
        List<StoreListItemResponse> content = page.getContent().stream()
                .map(s -> storeMapper.toListItem(s, resolveAssignedRoute(s.getId()), resolveAssignedRoutes(s.getId())))
                .toList();

        ApiResponse.PaginationInfo pagination = ApiResponse.PaginationInfo.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return ApiResponse.<List<StoreListItemResponse>>builder()
                .success(true)
                .data(content)
                .pagination(pagination)
                .build();
    }

    @Override
    @Transactional
    public StoreResponse updateStore(Long id, StoreUpdateRequest request) {
        Store store = findStoreOrThrow(id);
        validateCoordinates(request.getLatitude(), request.getLongitude());

        Province province = provinceRepository.findById(request.getProvinceCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVINCE_NOT_FOUND, "Province not found: " + request.getProvinceCode(), HttpStatus.BAD_REQUEST));
        District district = districtRepository.findById(request.getDistrictCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.DISTRICT_NOT_FOUND, "District not found: " + request.getDistrictCode(), HttpStatus.BAD_REQUEST));
        Ward ward = wardRepository.findById(request.getWardCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.WARD_NOT_FOUND, "Ward not found: " + request.getWardCode(), HttpStatus.BAD_REQUEST));

        if (district.getProvince() == null || !district.getProvince().getCode().equals(province.getCode())) {
            throw new BusinessException(ErrorCode.INVALID_ADDRESS, "District does not belong to the selected Province", HttpStatus.BAD_REQUEST);
        }
        if (ward.getDistrict() == null || !ward.getDistrict().getCode().equals(district.getCode())) {
            throw new BusinessException(ErrorCode.INVALID_ADDRESS, "Ward does not belong to the selected District", HttpStatus.BAD_REQUEST);
        }

        store.setName(request.getStoreName());
        store.setProvince(province);
        store.setDistrict(district);
        store.setWard(ward);
        store.setAddressDetail(request.getAddressDetail());
        store.setContactName(request.getContactName());
        store.setContactPhone(request.getContactPhone());
        store.setLatitude(request.getLatitude());
        store.setLongitude(request.getLongitude());
        store.setAllowedDeliveryHours(request.getAllowedDeliveryHours() != null ? request.getAllowedDeliveryHours() : "All");
        store.setMaxAllowedVehicleWeight(request.getMaxAllowedVehicleWeight());
        store.setImageUrl(request.getImageUrl());

        return buildStoreResponse(storeRepository.save(store));
    }

    @Override
    @Transactional
    public StoreResponse updateStoreStatus(Long id, StoreStatusUpdateRequest request) {
        Store store = findStoreOrThrow(id);

        if (!request.getIsActive() && routeStopRepository.existsByStoreIdAndRouteIsActiveTrue(id)) {
            String routeCode = routeStopRepository.findFirstByStoreId(id)
                    .map(rs -> rs.getRoute().getCode())
                    .orElse("unknown");
            throw new BusinessException(ErrorCode.STORE_ACTIVE_ROUTE,
                    "Store dang thuoc tuyen " + routeCode + ". Hay xoa store khoi tuyen truoc.",
                    HttpStatus.CONFLICT);
        }

        store.setIsActive(request.getIsActive());
        return buildStoreResponse(storeRepository.save(store));
    }

    // ── helpers ──────────────────────────────────────────────

    private Store findStoreOrThrow(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.STORE_NOT_FOUND, "Store not found: " + id, HttpStatus.NOT_FOUND));
    }

    private AssignedRouteDto resolveAssignedRoute(Long storeId) {
        return routeStopRepository.findFirstByStoreId(storeId)
                .map(rs -> AssignedRouteDto.builder()
                        .id(rs.getRoute().getId())
                        .code(rs.getRoute().getCode())
                        .name(rs.getRoute().getName())
                        .build())
                .orElse(null);
    }

    private List<AssignedRouteDto> resolveAssignedRoutes(Long storeId) {
        return routeStopRepository.findAllByStoreId(storeId).stream()
                .map(rs -> AssignedRouteDto.builder()
                        .id(rs.getRoute().getId())
                        .code(rs.getRoute().getCode())
                        .name(rs.getRoute().getName())
                        .build())
                .toList();
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BusinessException(ErrorCode.INVALID_COORDINATES,
                    "latitude va longitude phai duoc nhap ca hai hoac de trong ca hai",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private StoreResponse buildStoreResponse(Store store) {
        return storeMapper.toResponse(store, resolveAssignedRoute(store.getId()), resolveAssignedRoutes(store.getId()));
    }
}
