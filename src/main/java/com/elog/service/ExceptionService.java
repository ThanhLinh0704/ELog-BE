package com.elog.service;

import com.elog.dto.request.RejectStopRequest;
import com.elog.dto.request.ResolveExceptionRequest;
import com.elog.dto.response.DeliveryExceptionResponse;
import com.elog.dto.response.ExceptionListResponse;

import java.time.LocalDate;

public interface ExceptionService {

    DeliveryExceptionResponse rejectStop(Long tripStopId, RejectStopRequest request, String currentUsername);

    ExceptionListResponse listExceptions(LocalDate date, String type, String resolved);

    DeliveryExceptionResponse getException(Long exceptionId);

    DeliveryExceptionResponse resolveException(Long exceptionId, ResolveExceptionRequest request,
            String currentUsername);
}
