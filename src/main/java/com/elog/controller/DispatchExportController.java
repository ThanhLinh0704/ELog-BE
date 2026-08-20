package com.elog.controller;

import com.elog.service.DispatchExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/exports/confirmed-dispatch")
@RequiredArgsConstructor
@Tag(name = "Dispatch Export", description = "Confirmed Dispatch Data Export APIs (Xuất Dữ Liệu Điều Phối Đã Xác Nhận)")
public class DispatchExportController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DispatchExportService dispatchExportService;

    @GetMapping("/{tripDraftId}")
    @Operation(summary = "Export confirmed dispatch data for a single trip draft")
    @PreAuthorize("hasAnyAuthority('trip:coordinate', 'trip:read')")
    public ResponseEntity<byte[]> exportSingleDispatch(@PathVariable Long tripDraftId) {
        byte[] excelBytes = dispatchExportService.exportSingleDispatch(tripDraftId);
        String filename = String.format("confirmed-dispatch-%d.xlsx", tripDraftId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(excelBytes);
    }

    @GetMapping
    @Operation(summary = "Export confirmed dispatch data by date range (max 31 days)")
    @PreAuthorize("hasAnyAuthority('trip:coordinate', 'trip:read')")
    public ResponseEntity<byte[]> exportDispatchByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        byte[] excelBytes = dispatchExportService.exportDispatchByDateRange(fromDate, toDate);
        String filename = String.format("confirmed-dispatch-%s-to-%s.xlsx", fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(excelBytes);
    }
}
