package com.elog.service;

import com.elog.dto.response.ManifestByStopResponse;
import com.elog.dto.response.ManifestResponse;

public interface ManifestService {

    ManifestResponse generateManifest(Long tripDraftId, String currentUsername);

    ManifestResponse getManifest(Long tripDraftId);

    ManifestByStopResponse getManifestByStop(Long tripDraftId);
}
