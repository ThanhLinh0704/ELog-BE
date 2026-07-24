package com.elog.service.impl;

import com.elog.dto.response.*;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.ManifestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManifestServiceImpl implements ManifestService {

    private final TripDraftRepository tripDraftRepo;
    private final TripDraftStopRepository tripDraftStopRepo;
    private final OrderItemRepository orderItemRepo;
    private final ManifestRepository manifestRepo;
    private final ManifestLineRepository manifestLineRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional
    public ManifestResponse generateManifest(Long tripDraftId, String currentUsername) {
        // 1. Guard: TripDraft must exist
        TripDraft draft = tripDraftRepo.findById(tripDraftId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TRIP_DRAFT_NOT_FOUND,
                        "Trip Draft not found with id: " + tripDraftId,
                        HttpStatus.NOT_FOUND));

        // 2. Guard: TripDraft must be VALIDATED
        if (!"VALIDATED".equals(draft.getStatus())) {
            throw new BusinessException(
                    ErrorCode.TRIP_DRAFT_NOT_VALIDATED,
                    "Trip Draft " + tripDraftId + " must pass capacity validation (status=VALIDATED) before manifest generation. Current status: " + draft.getStatus() + ".",
                    HttpStatus.BAD_REQUEST);
        }

        // 3. Guard: no duplicate manifest
        if (manifestRepo.existsByTripDraftId(tripDraftId)) {
            throw new BusinessException(
                    ErrorCode.MANIFEST_ALREADY_EXISTS,
                    "Manifest already generated for Trip Draft " + tripDraftId + ". View via GET /manifest.",
                    HttpStatus.CONFLICT);
        }

        // 4. Get active stops sorted ascending
        List<TripDraftStop> activeStops = tripDraftStopRepo
                .findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(tripDraftId);

        if (activeStops.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.NO_ACTIVE_STOP,
                    "Trip Draft " + tripDraftId + " has no active stops.",
                    HttpStatus.BAD_REQUEST);
        }

        // 5. Reverse → LIFO order
        List<TripDraftStop> lifoStops = new ArrayList<>(activeStops);
        Collections.reverse(lifoStops);

        // 6. Resolve current user
        User currentUser = userRepo.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "User not found: " + currentUsername,
                        HttpStatus.NOT_FOUND));

        // 7. Create Manifest header first (to get ID)
        Manifest manifest = Manifest.builder()
                .tripDraft(draft)
                .generatedAt(LocalDateTime.now())
                .generatedBy(currentUser)
                .totalLines(0)
                .totalWeightKg(BigDecimal.ZERO)
                .totalVolumeM3(BigDecimal.ZERO)
                .build();
        manifestRepo.save(manifest);

        // 8. Build ManifestLines
        List<ManifestLine> lines = new ArrayList<>();
        int counter = 1;

        for (TripDraftStop stop : lifoStops) {
            List<OrderItem> items = orderItemRepo
                    .findByStopForManifest(stop.getStore().getId(), tripDraftId);

            if (items.isEmpty()) {
                log.warn("Active stop {} (store={}) has no order items — skipped in manifest",
                        stop.getId(), stop.getStore().getCode());
                continue;
            }

            for (OrderItem item : items) {
                ManifestLine line = ManifestLine.builder()
                        .manifest(manifest)
                        .lifoSequence(counter++)
                        .tripDraftStop(stop)
                        .orderItem(item)
                        .stopSequenceNo(stop.getSequenceNo())
                        .storeCode(stop.getStore().getCode())
                        .storeName(stop.getStore().getName())
                        .productCode(item.getProduct().getSku())
                        .productName(item.getProduct().getProductName())
                        .quantity(item.getQuantity())
                        .unitVolumeM3(item.getUnitVolumeM3())
                        .unitWeightKg(item.getUnitWeightKg())
                        .lineVolumeM3(item.getLineVolumeM3())
                        .lineWeightKg(item.getLineWeightKg())
                        .build();
                lines.add(line);
            }
        }

        // 9. Update manifest header totals
        BigDecimal totalWeight = lines.stream()
                .map(ManifestLine::getLineWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVolume = lines.stream()
                .map(ManifestLine::getLineVolumeM3)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        manifest.setTotalLines(lines.size());
        manifest.setTotalWeightKg(totalWeight);
        manifest.setTotalVolumeM3(totalVolume);

        // 10. Batch insert lines
        manifestLineRepo.saveAll(lines);
        manifestRepo.save(manifest);

        // 11. Build response
        return ManifestResponse.builder()
                .manifestId(manifest.getManifestId())
                .tripDraftId(tripDraftId)
                .fixedRouteCode(draft.getRoute().getCode())
                .deliveryDate(draft.getDeliveryDate().toString())
                .generatedAt(manifest.getGeneratedAt())
                .generatedBy(ConfirmedByDto.builder()
                        .userId(currentUser.getId())
                        .fullName(currentUser.getFullName())
                        .build())
                .totalLines(manifest.getTotalLines())
                .totalWeightKg(totalWeight)
                .totalVolumeM3(totalVolume)
                .message("LIFO manifest generated. " + lines.size() + " manifest lines created for " + activeStops.size() + " active stops.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ManifestResponse getManifest(Long tripDraftId) {
        Manifest manifest = manifestRepo.findByTripDraftIdWithDetails(tripDraftId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MANIFEST_NOT_FOUND,
                        "Manifest not found for Trip Draft " + tripDraftId + ". Call POST /generate-manifest first.",
                        HttpStatus.NOT_FOUND));

        List<ManifestLine> lines = manifestLineRepo
                .findByManifestManifestIdOrderByLifoSequenceAsc(manifest.getManifestId());

        List<ManifestLineDto> lineDtos = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            ManifestLine line = lines.get(i);
            String loadingNote = null;
            if (i == 0) {
                loadingNote = "Xếp VÀO xe đầu tiên — nằm sâu nhất";
            } else if (i == lines.size() - 1) {
                loadingNote = "Xếp VÀO xe cuối cùng — gần cửa xe nhất";
            }

            lineDtos.add(ManifestLineDto.builder()
                    .lifoSequence(line.getLifoSequence())
                    .stopSequenceNo(line.getStopSequenceNo())
                    .storeCode(line.getStoreCode())
                    .storeName(line.getStoreName())
                    .productCode(line.getProductCode())
                    .productName(line.getProductName())
                    .quantity(line.getQuantity())
                    .unitWeightKg(line.getUnitWeightKg())
                    .unitVolumeM3(line.getUnitVolumeM3())
                    .lineWeightKg(line.getLineWeightKg())
                    .lineVolumeM3(line.getLineVolumeM3())
                    .loadingNote(loadingNote)
                    .build());
        }

        TripDraft draft = manifest.getTripDraft();
        User generator = manifest.getGeneratedBy();

        return ManifestResponse.builder()
                .manifestId(manifest.getManifestId())
                .tripDraftId(draft.getId())
                .fixedRouteCode(draft.getRoute().getCode())
                .deliveryDate(draft.getDeliveryDate().toString())
                .generatedAt(manifest.getGeneratedAt())
                .generatedBy(ConfirmedByDto.builder()
                        .userId(generator.getId())
                        .fullName(generator.getFullName())
                        .build())
                .totalLines(manifest.getTotalLines())
                .totalWeightKg(manifest.getTotalWeightKg())
                .totalVolumeM3(manifest.getTotalVolumeM3())
                .lines(lineDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ManifestByStopResponse getManifestByStop(Long tripDraftId) {
        Manifest manifest = manifestRepo.findByTripDraftIdWithDetails(tripDraftId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MANIFEST_NOT_FOUND,
                        "Manifest not found for Trip Draft " + tripDraftId + ". Call POST /generate-manifest first.",
                        HttpStatus.NOT_FOUND));

        // Sort by stop_sequence_no ASC (delivery order), then lifo_sequence ASC within each stop
        List<ManifestLine> lines = manifestLineRepo
                .findByManifestManifestIdOrderByStopSequenceNoAscLifoSequenceAsc(manifest.getManifestId());

        // Group by stopSequenceNo while preserving insertion order
        Map<Integer, List<ManifestLine>> groupedByStop = lines.stream()
                .collect(Collectors.groupingBy(ManifestLine::getStopSequenceNo, LinkedHashMap::new, Collectors.toList()));


        List<ManifestByStopResponse.ManifestStopGroup> stopGroups = new ArrayList<>();

        // Iterate in LIFO loading order (reverse of delivery order = descending sequence_no)
        // — Nhóm 1 = stop xếp vào đầu tiên (sequence_no lớn nhất)
        List<Integer> sortedSeqNos = new ArrayList<>(groupedByStop.keySet());
        Collections.sort(sortedSeqNos, Collections.reverseOrder());

        int groupIndex = 1;
        int totalGroups = sortedSeqNos.size();

        for (Integer seqNo : sortedSeqNos) {
            List<ManifestLine> stopLines = groupedByStop.get(seqNo);
            ManifestLine firstLine = stopLines.get(0);

            String loadingNote;
            if (groupIndex == 1) {
                loadingNote = "Nhóm này xếp VÀO xe ĐẦU TIÊN";
            } else if (groupIndex == totalGroups) {
                loadingNote = "Nhóm này xếp VÀO xe CUỐI CÙNG — gần cửa";
            } else {
                loadingNote = null;
            }

            BigDecimal stopWeight = stopLines.stream()
                    .map(ManifestLine::getLineWeightKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal stopVolume = stopLines.stream()
                    .map(ManifestLine::getLineVolumeM3)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<ManifestLineDto> itemDtos = stopLines.stream()
                    .map(l -> ManifestLineDto.builder()
                            .lifoSequence(l.getLifoSequence())
                            .productCode(l.getProductCode())
                            .productName(l.getProductName())
                            .quantity(l.getQuantity())
                            .unitWeightKg(l.getUnitWeightKg())
                            .unitVolumeM3(l.getUnitVolumeM3())
                            .lineWeightKg(l.getLineWeightKg())
                            .lineVolumeM3(l.getLineVolumeM3())
                            .build())
                    .collect(Collectors.toList());

            stopGroups.add(ManifestByStopResponse.ManifestStopGroup.builder()
                    .stopSequenceNo(seqNo)
                    .storeCode(firstLine.getStoreCode())
                    .storeName(firstLine.getStoreName())
                    .loadingNote(loadingNote)
                    .items(itemDtos)
                    .stopWeightKg(stopWeight)
                    .stopVolumeM3(stopVolume)
                    .build());

            groupIndex++;
        }

        TripDraft draft = manifest.getTripDraft();

        return ManifestByStopResponse.builder()
                .manifestId(manifest.getManifestId())
                .tripDraftId(draft.getId())
                .fixedRouteCode(draft.getRoute().getCode())
                .deliveryDate(draft.getDeliveryDate().toString())
                .stops(stopGroups)
                .build();
    }
}
