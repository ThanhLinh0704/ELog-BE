package com.elog.service;

import com.elog.dto.response.trip.TripOutcomeResponse;
import com.elog.entity.TripOutcome;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.TripOutcomeRepository;
import com.elog.service.impl.TripOutcomeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripOutcomeServiceImplTest {
    @Mock TripOutcomeRepository repository; @Mock TripOutcomeHistoryService historyService;
    TripOutcomeServiceImpl service;
    @BeforeEach void setUp(){service=new TripOutcomeServiceImpl(repository,historyService);}
    private TripOutcome outcome(String status){return TripOutcome.builder().id(5L).status(status).version(1).totalOrders(2).deliveredCount(2).failedCount(0).partialCount(0).build();}

    private void assertValidation(String initialStatus) {
        TripOutcome value=outcome(initialStatus); when(repository.findById(5L)).thenReturn(Optional.of(value)); when(repository.save(value)).thenReturn(value);
        LocalDateTime before=LocalDateTime.now(); TripOutcomeResponse response=service.validateOutcome(5L,"dispatcher");
        assertAll(()->assertEquals("VALIDATED",value.getStatus()),()->assertEquals("dispatcher",value.getValidatedBy()),
                ()->assertNotNull(value.getValidatedAt()),()->assertFalse(value.getValidatedAt().isBefore(before)),()->assertEquals("VALIDATED",response.getStatus()));
        verify(repository).save(value); verify(historyService).record(any());
    }

    @Test @DisplayName("[L1-TO-04] dispatcher validates a submitted outcome") void submittedOutcomeIsValidated(){assertValidation("SUBMITTED");}
    @Test @DisplayName("[L1-TO-05] dispatcher validates an outcome needing correction") void correctedOutcomeIsValidated(){assertValidation("NEEDS_CORRECTION");}

    @Test @DisplayName("[L1-TO-06] missing outcome returns RESOURCE_NOT_FOUND")
    void missingOutcomeIsRejected(){when(repository.findById(999L)).thenReturn(Optional.empty()); BusinessException e=assertThrows(BusinessException.class,()->service.validateOutcome(999L,"dispatcher")); assertEquals(ErrorCode.RESOURCE_NOT_FOUND,e.getErrorCode());}

    @Test @DisplayName("[L1-TO-07] already validated outcome is rejected without another save")
    void invalidOutcomeStateIsRejected(){TripOutcome value=outcome("VALIDATED"); when(repository.findById(5L)).thenReturn(Optional.of(value)); BusinessException e=assertThrows(BusinessException.class,()->service.validateOutcome(5L,"dispatcher")); assertEquals(ErrorCode.VALIDATION_FAILED,e.getErrorCode()); verify(repository,never()).save(any());}

    @Test @DisplayName("[L1-TO-08] amendment appends history and increments outcome version")
    void amendmentRecordsNewVersion(){TripOutcome value=outcome("VALIDATED"); when(repository.findById(5L)).thenReturn(Optional.of(value)); when(repository.save(value)).thenReturn(value);
        TripOutcomeResponse response=service.amendOutcome(5L,"incorrect count","dispatcher");
        assertAll(()->assertEquals("NEEDS_CORRECTION",value.getStatus()),()->assertEquals("incorrect count",value.getAmendmentReason()),()->assertEquals(2,value.getVersion()),()->assertEquals(2,response.getVersion()));
        ArgumentCaptor<TripOutcomeHistoryService.OutcomeEventInput> event=ArgumentCaptor.forClass(TripOutcomeHistoryService.OutcomeEventInput.class); verify(historyService).record(event.capture()); assertEquals("incorrect count",event.getValue().validationNote()); verify(repository).save(value);}

    @Test @DisplayName("[L1-TO-09] blank amendment reason leaves status and version unchanged")
    void blankAmendmentIsRejectedWithoutMutation(){TripOutcome value=outcome("VALIDATED"); when(repository.findById(5L)).thenReturn(Optional.of(value)); BusinessException e=assertThrows(BusinessException.class,()->service.amendOutcome(5L,"  ","dispatcher"));
        assertAll(()->assertEquals(ErrorCode.VALIDATION_FAILED,e.getErrorCode()),()->assertEquals("VALIDATED",value.getStatus()),()->assertEquals(1,value.getVersion())); verify(repository,never()).save(any()); verifyNoInteractions(historyService);}

    @Test
    @DisplayName("[L1-TO-10] getSubmittedOutcomes returns list of submitted outcomes")
    void getSubmittedOutcomesSuccess() {
        TripOutcome value = outcome("SUBMITTED");
        when(repository.findByStatus("SUBMITTED")).thenReturn(List.of(value));

        var list = service.getSubmittedOutcomes();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("SUBMITTED", list.get(0).getStatus());
    }
}

