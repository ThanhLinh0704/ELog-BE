package com.elog.service;

import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.ImportBatchResponse;
import com.elog.dto.response.ImportErrorResponse;
import com.elog.dto.response.ImportedOrderDetailResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface ImportService {

    ImportBatchResponse importExcel(MultipartFile file, LocalDate deliveryDate,
                                    boolean confirmReplace, Long uploadedBy);

    ApiResponse<List<ImportBatchResponse>> getBatches(LocalDate deliveryDate, Pageable pageable);

    ImportBatchResponse getBatchById(Long batchId);

    ApiResponse<List<ImportErrorResponse>> getBatchErrors(Long batchId, String errorCode, Pageable pageable);

    byte[] exportBatchErrors(Long batchId);

    List<ImportedOrderDetailResponse> getImportedOrders(Long batchId);
}
