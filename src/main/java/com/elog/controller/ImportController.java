package com.elog.controller;

import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.ImportBatchResponse;
import com.elog.dto.response.ImportErrorResponse;
import com.elog.dto.response.ImportedOrderDetailResponse;
import com.elog.entity.User;
import com.elog.repository.UserRepository;
import com.elog.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/imports")
@RequiredArgsConstructor
@Tag(name = "Import", description = "Excel import APIs for order management")
public class ImportController {

    private final ImportService importService;
    private final UserRepository userRepository;

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Upload Excel file to import orders for a delivery date")
    @PreAuthorize("hasAuthority('order:import')")
    public ResponseEntity<?> importOrders(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "deliveryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(value = "confirmReplace", required = false, defaultValue = "false") boolean confirmReplace,
            Authentication authentication) {

        // Get user ID from authenticated username
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        Long userId = user.getId();

        ImportBatchResponse response = importService.importExcel(file, deliveryDate, confirmReplace, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Import hoàn tất"));
    }

    @GetMapping
    @Operation(summary = "Get import batch history (paginated, optionally filter by date)")
    @PreAuthorize("hasAnyAuthority('order:import', 'trip:read')")
    public ResponseEntity<ApiResponse<List<ImportBatchResponse>>> getBatches(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @PageableDefault(size = 20) Pageable pageable) {

        ApiResponse<List<ImportBatchResponse>> response = importService.getBatches(deliveryDate, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "Get import batch detail by ID")
    @PreAuthorize("hasAnyAuthority('order:import', 'trip:read')")
    public ResponseEntity<ApiResponse<ImportBatchResponse>> getBatchById(@PathVariable Long batchId) {
        ImportBatchResponse response = importService.getBatchById(batchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{batchId}/orders")
    @Operation(summary = "Get list of successfully imported orders and products details for a batch")
    @PreAuthorize("hasAnyAuthority('order:import', 'trip:read')")
    public ResponseEntity<ApiResponse<List<ImportedOrderDetailResponse>>> getImportedOrders(
            @PathVariable Long batchId,
            @RequestParam(value = "deliveryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate) {
        List<ImportedOrderDetailResponse> response = importService.getImportedOrders(batchId, deliveryDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{batchId}/errors")
    @Operation(summary = "Get error list for a specific import batch (paginated & filterable)")
    @PreAuthorize("hasAnyAuthority('order:import', 'trip:read')")
    public ResponseEntity<ApiResponse<List<ImportErrorResponse>>> getBatchErrors(
            @PathVariable Long batchId,
            @RequestParam(value = "errorCode", required = false) String errorCode,
            @PageableDefault(size = 20) Pageable pageable) {
        ApiResponse<List<ImportErrorResponse>> response = importService.getBatchErrors(batchId, errorCode, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{batchId}/errors/export")
    @Operation(summary = "Export error list to Excel file")
    @PreAuthorize("hasAnyAuthority('order:import', 'trip:read')")
    public ResponseEntity<byte[]> exportBatchErrors(@PathVariable Long batchId) {
        byte[] content = importService.exportBatchErrors(batchId);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType
                .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "import-errors-batch" + batchId + ".xlsx");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(content, headers, org.springframework.http.HttpStatus.OK);
    }
}
