package com.elog.service;

import java.time.LocalDate;

/**
 * Confirmed Dispatch Data Export Service (Xuất Dữ Liệu Điều Phối Đã Xác Nhận).
 */
public interface DispatchExportService {

    /**
     * Export confirmed dispatch data for a single trip draft / dispatch group.
     *
     * @param tripDraftId the TripDraft ID
     * @return binary Excel file content (.xlsx)
     */
    byte[] exportSingleDispatch(Long tripDraftId);

    /**
     * Export confirmed dispatch data for all trips within a date range (max 31 days).
     *
     * @param fromDate start delivery date (inclusive)
     * @param toDate   end delivery date (inclusive)
     * @return binary Excel file content (.xlsx)
     */
    byte[] exportDispatchByDateRange(LocalDate fromDate, LocalDate toDate);
}
