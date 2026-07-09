package com.elog.service;

import com.elog.dto.response.CapacityValidationResultResponse;

public interface CapacityValidationService {
    CapacityValidationResultResponse validate(Long tripDraftId, String currentUsername);
    CapacityValidationResultResponse getValidationResult(Long tripDraftId);
}
