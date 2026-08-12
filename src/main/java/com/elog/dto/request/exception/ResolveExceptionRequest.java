package com.elog.dto.request.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResolveExceptionRequest {

    private String resolutionNotes; // Optional — ghi chú giải quyết của Dispatcher/Manager
}
