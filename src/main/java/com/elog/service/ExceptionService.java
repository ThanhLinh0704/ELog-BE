package com.elog.service;

import com.elog.dto.request.exception.RejectStopRequest;
import com.elog.dto.request.exception.ResolveExceptionRequest;
import com.elog.dto.response.exception.DeliveryExceptionResponse;
import com.elog.dto.response.exception.ExceptionListResponse;

import java.time.LocalDate;

public interface ExceptionService {

    DeliveryExceptionResponse rejectStop(Long tripStopId, RejectStopRequest request, String currentUsername);

    ExceptionListResponse listExceptions(LocalDate date, String type, String resolved);

    ExceptionListResponse listExceptionsInRange(LocalDate startDate, LocalDate endDate, String type, String resolved);

    DeliveryExceptionResponse getException(Long exceptionId);

    DeliveryExceptionResponse resolveException(Long exceptionId, ResolveExceptionRequest request,
            String currentUsername);
}
