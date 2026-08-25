package com.elog.service.impl;

import com.elog.dto.request.vehicle.VehicleCreateRequest;
import com.elog.dto.request.vehicle.VehicleStatusUpdateRequest;
import com.elog.dto.request.vehicle.VehicleUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.vehicle.VehicleCurrentTripResponse;
import com.elog.dto.response.vehicle.VehicleFleetCapacityResponse;
import com.elog.dto.response.vehicle.VehicleListItemResponse;
import com.elog.dto.response.vehicle.VehicleResponse;
import com.elog.entity.Trip;
import com.elog.entity.TripExecution;
import com.elog.entity.TripStatus;
import com.elog.entity.TripStop;
import com.elog.entity.Vehicle;
import com.elog.entity.VehicleStatus;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.VehicleMapper;
import com.elog.repository.TripExecutionRepository;
import com.elog.repository.TripRepository;
import com.elog.repository.VehicleRepository;
import com.elog.repository.specification.VehicleSpecification;
import com.elog.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.elog.entity.User;
import com.elog.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private static final List<TripStatus> UNCOMPLETED_STATUSES =
            List.of(TripStatus.VALIDATED, TripStatus.DISPATCHED, TripStatus.IN_PROGRESS);

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final VehicleMapper vehicleMapper;
    private final TripRepository tripRepository;
    private final TripExecutionRepository tripExecutionRepository;

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleCreateRequest request) {
        String normalizedPlate = vehicleMapper.normalizePlate(request.getPlateNumber());
        if (vehicleRepository.existsByPlateNumber(normalizedPlate)) {
            throw new BusinessException(ErrorCode.VEHICLE_PLATE_DUPLICATE,
                    "Vehicle plate already exists: " + normalizedPlate, HttpStatus.CONFLICT);
        }

        if (request.getVehicleCode() != null && vehicleRepository.existsByVehicleCode(request.getVehicleCode().trim())) {
            throw new BusinessException(ErrorCode.VEHICLE_CODE_DUPLICATE,
                    "Vehicle code already exists: " + request.getVehicleCode().trim(), HttpStatus.CONFLICT);
        }

        validateVehicleCapacityRatio(request.getPayloadKg(), request.getMaxVolumeM3());

        Vehicle vehicle = vehicleMapper.toEntity(request);
        if (request.getAssignedDriverId() != null) {
            User driver = userRepository.findById(request.getAssignedDriverId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Driver not found: " + request.getAssignedDriverId(), HttpStatus.NOT_FOUND));
            // Unassign driver from any other vehicle
            List<Vehicle> currentlyAssigned = vehicleRepository.findByAssignedDriverId(driver.getId());
            for (Vehicle otherV : currentlyAssigned) {
                otherV.setAssignedDriver(null);
                vehicleRepository.save(otherV);
            }
            vehicle.setAssignedDriver(driver);
        }
        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = findOrThrow(id);
        VehicleResponse response = vehicleMapper.toResponse(vehicle);
        response.setCurrentTrip(buildCurrentTrip(vehicle.getId()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<VehicleListItemResponse>> getAllVehicles(
            String keyword,
            Boolean isActive,
            VehicleStatus status,
            BigDecimal minWeightKg,
            BigDecimal maxWeightKg,
            BigDecimal minVolumeM3,
            Pageable pageable,
            LocalDate date) {

        Specification<Vehicle> spec = Specification
                .where(VehicleSpecification.hasKeyword(keyword))
                .and(VehicleSpecification.hasActiveStatus(isActive))
                .and(VehicleSpecification.hasStatus(status))
                .and(VehicleSpecification.hasMinimumWeight(minWeightKg))
                .and(VehicleSpecification.hasMaximumWeight(maxWeightKg))
                .and(VehicleSpecification.hasMinimumVolume(minVolumeM3));

        Page<Vehicle> page = vehicleRepository.findAll(spec, pageable);

        List<VehicleListItemResponse> items = page.getContent().stream()
                .map(v -> {
                    VehicleListItemResponse item = vehicleMapper.toListItem(v);
                    item.setCurrentTrip(date != null ? buildTripStatusForDate(v.getId(), date) : buildCurrentTrip(v.getId()));
                    return item;
                })
                .toList();

        return ApiResponse.<List<VehicleListItemResponse>>builder()
                .success(true)
                .data(items)
                .pagination(ApiResponse.PaginationInfo.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleFleetCapacityResponse getFleetCapacity() {
        return VehicleFleetCapacityResponse.builder()
                .activeVehicleCount(vehicleRepository.countByIsActiveTrue())
                .totalMaxWeightKg(zeroIfNull(vehicleRepository.sumActiveMaxWeightKg()))
                .totalMaxVolumeM3(zeroIfNull(vehicleRepository.sumActiveMaxVolumeM3()))
                .build();
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleUpdateRequest request) {
        Vehicle vehicle = findOrThrow(id);

        // plateNumber is intentionally immutable. If a real plate changes, deactivate
        // the old vehicle and create a new vehicle record to preserve trip history.
        validateVehicleCapacityRatio(request.getPayloadKg(), request.getMaxVolumeM3());
        assertStatusChangeAllowed(vehicle, request.getStatus(), request.getIsActive());

        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setVehicleClass(request.getVehicleClass());
        vehicle.setPayloadKg(request.getPayloadKg());
        vehicle.setGrossVehicleWeightKg(request.getGrossVehicleWeightKg());
        vehicle.setRequiredLicense(request.getRequiredLicense());
        vehicle.setMaxVolumeM3(request.getMaxVolumeM3());
        vehicle.setCargoLengthMm(request.getCargoLengthMm());
        vehicle.setCargoWidthMm(request.getCargoWidthMm());
        vehicle.setCargoHeightMm(request.getCargoHeightMm());
        vehicle.setAverageSpeedKmh(request.getAverageSpeedKmh());
        vehicle.setCostPerKm(request.getCostPerKm());
        if (request.getStatus() != null) {
            vehicle.setStatus(request.getStatus());
        }
        if (request.getIsActive() != null) {
            vehicle.setIsActive(request.getIsActive());
        }
        vehicle.setImageUrl(request.getImageUrl());
        vehicle.setPermitInfo(request.getPermitInfo());
        vehicle.setDescription(request.getDescription());

        if (request.getAssignedDriverId() != null) {
            User driver = userRepository.findById(request.getAssignedDriverId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Driver not found: " + request.getAssignedDriverId(), HttpStatus.NOT_FOUND));
            // Unassign driver from any other vehicle
            List<Vehicle> currentlyAssigned = vehicleRepository.findByAssignedDriverId(driver.getId());
            for (Vehicle otherV : currentlyAssigned) {
                if (!otherV.getId().equals(vehicle.getId())) {
                    otherV.setAssignedDriver(null);
                    vehicleRepository.save(otherV);
                }
            }
            vehicle.setAssignedDriver(driver);
        } else {
            vehicle.setAssignedDriver(null);
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicleStatus(Long id, VehicleStatusUpdateRequest request) {
        Vehicle vehicle = findOrThrow(id);

        assertStatusChangeAllowed(vehicle, null, request.getIsActive());
        vehicle.setIsActive(request.getIsActive());

        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> findAvailableVehiclesForTrip(Long tripId) {
        List<Vehicle> availableVehicles = vehicleRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .filter(v -> v.getStatus() == com.elog.entity.VehicleStatus.AVAILABLE)
                .toList();

        return availableVehicles.stream()
                .map(vehicleMapper::toResponse)
                .toList();
    }

    /**
     * Chuyến hiện tại của xe (nếu có) — xem filemd/FLEET-STATUS-DASHBOARD-ADJUSTED-SPEC.md mục 0.
     * Vehicle.status = IN_USE gộp chung cả 4 giai đoạn Assigned/Dispatched/InProgress/Returning;
     * field này tách lại đúng giai đoạn bằng cách join Trip + TripExecution của xe.
     */
    private VehicleCurrentTripResponse buildCurrentTrip(Long vehicleId) {
        List<Trip> uncompletedTrips = tripRepository.findByVehicleIdAndStatusIn(vehicleId, UNCOMPLETED_STATUSES);

        Trip trip;
        String phase;
        if (!uncompletedTrips.isEmpty()) {
            trip = uncompletedTrips.get(0);
            phase = phaseForUncompletedTrip(trip);
        } else {
            List<TripExecution> unreturned = tripExecutionRepository.findUnreturnedByVehicleId(vehicleId);
            trip = unreturned.isEmpty() ? null : unreturned.get(0).getTrip();
            phase = trip != null ? "RETURNING" : null;
        }

        return toCurrentTripResponse(trip, phase);
    }

    /**
     * Chuyến của xe vào đúng 1 ngày cụ thể — khác {@link #buildCurrentTrip}, không phải "chuyến
     * đang treo hiện tại" mà là "hôm đó xe này đã chạy chuyến gì, tới đâu rồi", kể cả chuyến đã
     * hoàn thành VÀ đã xác nhận về kho (COMPLETED_RETURNED — buildCurrentTrip không bao giờ trả
     * phase này vì lúc đó nó coi như xe đã rảnh, không còn "chuyến hiện tại" nữa).
     */
    private VehicleCurrentTripResponse buildTripStatusForDate(Long vehicleId, LocalDate date) {
        List<Trip> tripsOnDate = tripRepository.findByVehicleIdAndDeliveryDate(vehicleId, date);
        if (tripsOnDate.isEmpty()) {
            // Không có chuyến nào lên lịch đúng ngày được xem — nhưng nếu đang xem hôm nay/tương lai,
            // xe vẫn có thể đang bận vì lý do KHÁC ngày đó: hoặc còn 1 chuyến ngày trước chưa xong/
            // chưa xác nhận về kho, hoặc (hiếm hơn) 1 chuyến ngày trước chưa bắt đầu. Dùng lại đúng
            // logic buildCurrentTrip (ưu tiên chuyến CHƯA HOÀN THÀNH trước, chỉ mới coi là RETURNING
            // khi thật sự không còn chuyến nào đang treo) — nếu không sẽ nhầm 1 chuyến DISPATCHED
            // chưa từng chạy (execution vẫn ASSIGNED, returnedToWarehouseAt cũng NULL) thành RETURNING.
            // Bỏ qua toàn bộ fallback này khi xem NGÀY TRONG QUÁ KHỨ (audit lịch sử), vì lúc đó "xe
            // hiện tại đang làm gì" không liên quan gì tới đúng ngày lịch sử đang xem.
            return date.isBefore(LocalDate.now()) ? null : buildFallbackTripStatus(vehicleId, date);
        }
        // 1 xe có thể có >1 Trip cùng ngày kể từ khi có tính năng Huỷ chuyến (xe được giải phóng ngay
        // sau khi huỷ nên có thể được gán 1 Trip mới cùng ngày). Chuyến CANCELLED không còn ý nghĩa gì
        // với ngày đó nữa — ưu tiên Trip chưa huỷ mới nhất (tripId lớn nhất); nếu tất cả đều đã huỷ thì
        // mới hiện chuyến huỷ gần nhất, còn hơn không có gì.
        Trip trip = tripsOnDate.stream()
                .filter(t -> t.getStatus() != TripStatus.CANCELLED)
                .max(Comparator.comparing(Trip::getTripId))
                .orElseGet(() -> tripsOnDate.stream().max(Comparator.comparing(Trip::getTripId)).orElseThrow());

        String phase;
        if (trip.getStatus() != TripStatus.COMPLETED) {
            phase = phaseForUncompletedTrip(trip);
        } else {
            boolean returned = tripExecutionRepository.findByTripId(trip.getTripId())
                    .map(te -> te.getReturnedToWarehouseAt() != null)
                    .orElse(false);
            phase = returned ? "COMPLETED_RETURNED" : "RETURNING";
        }

        return toCurrentTripResponse(trip, phase);
    }

    /**
     * Fallback riêng cho {@link #buildTripStatusForDate} — CHỈ bắt "1 chuyến ngày trước tràn sang"
     * (deliveryDate <= ngày đang xem), khác {@link #buildCurrentTrip} (không giới hạn ngày, dùng cho
     * truy vấn không có ngữ cảnh ngày cụ thể). Nếu dùng buildCurrentTrip ở đây, 1 xe đã DISPATCHED
     * cho chuyến 30/08 sẽ bị hiện nhầm "Đã điều phối" khi xem Fleet Dashboard ở NGÀY 26/08 — dù chuyến
     * đó chưa liên quan gì tới ngày đang xem.
     */
    private VehicleCurrentTripResponse buildFallbackTripStatus(Long vehicleId, LocalDate viewedDate) {
        List<Trip> uncompletedTrips = tripRepository.findByVehicleIdAndStatusIn(vehicleId, UNCOMPLETED_STATUSES).stream()
                .filter(t -> t.getDeliveryDate() == null || !t.getDeliveryDate().isAfter(viewedDate))
                .toList();

        Trip trip;
        String phase;
        if (!uncompletedTrips.isEmpty()) {
            trip = uncompletedTrips.get(0);
            phase = phaseForUncompletedTrip(trip);
        } else {
            List<TripExecution> unreturned = tripExecutionRepository.findUnreturnedByVehicleId(vehicleId).stream()
                    .filter(te -> te.getTrip() == null || te.getTrip().getDeliveryDate() == null || !te.getTrip().getDeliveryDate().isAfter(viewedDate))
                    .toList();
            trip = unreturned.isEmpty() ? null : unreturned.get(0).getTrip();
            phase = trip != null ? "RETURNING" : null;
        }

        return toCurrentTripResponse(trip, phase);
    }

    private String phaseForUncompletedTrip(Trip trip) {
        return switch (trip.getStatus()) {
            case VALIDATED -> "ASSIGNED";
            case DISPATCHED -> "DISPATCHED";
            case IN_PROGRESS -> "IN_PROGRESS";
            case COMPLETED -> "RETURNING"; // callers only pass non-COMPLETED trips here
            case CANCELLED -> "CANCELLED"; // only reachable via buildTripStatusForDate (date-scoped view)
        };
    }

    private VehicleCurrentTripResponse toCurrentTripResponse(Trip trip, String phase) {
        if (trip == null) {
            return null;
        }

        java.time.LocalDateTime estimatedCompletionAt = trip.getStops().stream()
                .map(TripStop::getPlannedEta)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return VehicleCurrentTripResponse.builder()
                .tripId(trip.getTripId())
                .routeCode(trip.getRoute() != null ? trip.getRoute().getCode() : null)
                .driverName(trip.getDriver() != null ? trip.getDriver().getFullName() : null)
                .deliveryDate(trip.getDeliveryDate())
                .phase(phase)
                .estimatedCompletionAt(estimatedCompletionAt)
                .build();
    }

    private Vehicle findOrThrow(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND,
                        "Vehicle not found with id: " + id, HttpStatus.NOT_FOUND));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validateVehicleCapacityRatio(BigDecimal maxWeightKg, BigDecimal maxVolumeM3) {
        if (maxWeightKg != null && maxVolumeM3 != null) {
            double volume = maxVolumeM3.doubleValue();
            if (volume > 0) {
                double ratio = maxWeightKg.doubleValue() / volume;
                if (ratio < 80.0 || ratio > 1500.0) {
                    throw new BusinessException(ErrorCode.INVALID_CAPACITY_RATIO,
                            "Tỷ lệ tải trọng/thể tích của xe không hợp lý (80-1500 kg/m³). Khai báo hiện tại: " + Math.round(ratio) + " kg/m³.",
                            HttpStatus.BAD_REQUEST);
                }
            }
        }
    }

    /**
     * Xe đang IN_USE (đang thực hiện chuyến) — không cho đổi status/isActive qua form sửa xe hay
     * nút khóa cho tới khi tài xế xác nhận về kho (lúc đó BE tự chuyển lại AVAILABLE). Chỉ chặn
     * khi request thật sự MUỐN đổi sang giá trị khác — submit lại đúng giá trị hiện tại (vd sửa
     * các field khác trong form, không đụng vào status) vẫn cho qua bình thường.
     */
    private void assertStatusChangeAllowed(Vehicle vehicle, VehicleStatus requestedStatus, Boolean requestedIsActive) {
        if (vehicle.getStatus() != VehicleStatus.IN_USE) {
            return;
        }
        boolean statusChanging = requestedStatus != null && requestedStatus != VehicleStatus.IN_USE;
        boolean deactivating = Boolean.FALSE.equals(requestedIsActive);
        if (statusChanging || deactivating) {
            throw new BusinessException(ErrorCode.VEHICLE_STATUS_LOCKED,
                    "Vehicle " + vehicle.getPlateNumber()
                            + " is currently IN_USE (executing a trip) — status cannot be changed until the driver confirms return to warehouse.",
                    HttpStatus.CONFLICT);
        }
    }
}
