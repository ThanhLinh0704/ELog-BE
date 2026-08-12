package com.elog.service;

import com.elog.dto.response.trip.ManifestByStopResponse;
import com.elog.dto.response.trip.ManifestResponse;

public interface ManifestService {

    ManifestResponse generateManifest(Long tripDraftId, String currentUsername);

    ManifestResponse getManifest(Long tripDraftId);

    ManifestByStopResponse getManifestByStop(Long tripDraftId);
}
